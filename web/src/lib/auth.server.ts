import { type BetterAuthOptions, betterAuth } from "better-auth";
import { APIError } from "better-auth/api";
import { organization } from "better-auth/plugins";
import { tanstackStartCookies } from "better-auth/tanstack-start";
import { Pool } from "pg";
import { authSettings } from "./api-config.server";

/**
 * Better Auth on our Postgres (ADR 0038, 0042, 0043). Flyway owns the tables (V8__auth.sql), so every model and
 * field is mapped to its snake_case name here; `auth-schema.server.test.ts` fails when the two drift apart.
 * Built on first use, so the fixture build never needs a database.
 */
export function authOptions(pool: Pool, secret: string, baseUrl: string) {
	return {
		database: pool,
		secret,
		baseURL: baseUrl,
		emailAndPassword: { enabled: true, disableSignUp: true },
		user: {
			modelName: "app_user",
			fields: { emailVerified: "email_verified", createdAt: "created_at", updatedAt: "updated_at" },
		},
		session: {
			modelName: "auth_session",
			fields: {
				userId: "user_id",
				expiresAt: "expires_at",
				ipAddress: "ip_address",
				userAgent: "user_agent",
				createdAt: "created_at",
				updatedAt: "updated_at",
			},
		},
		account: {
			modelName: "auth_account",
			fields: {
				userId: "user_id",
				accountId: "account_id",
				providerId: "provider_id",
				accessToken: "access_token",
				refreshToken: "refresh_token",
				idToken: "id_token",
				accessTokenExpiresAt: "access_token_expires_at",
				refreshTokenExpiresAt: "refresh_token_expires_at",
				createdAt: "created_at",
				updatedAt: "updated_at",
			},
		},
		verification: {
			modelName: "auth_verification",
			fields: { expiresAt: "expires_at", createdAt: "created_at", updatedAt: "updated_at" },
		},
		databaseHooks: {
			session: {
				create: {
					// A session starts in the user's oldest organization, and nobody without one gets a session,
					// because every API call needs an organization (ADR 0043).
					before: async (session) => {
						const { rows } = await pool.query<{ organization_id: string }>(
							"select organization_id from member where user_id = $1 order by created_at, id limit 1",
							[session.userId],
						);
						if (rows.length === 0) {
							throw new APIError("FORBIDDEN", { message: "Esta conta não pertence a nenhuma organização." });
						}
						return { data: { ...session, activeOrganizationId: rows[0].organization_id } };
					},
				},
			},
		},
		plugins: [
			organization({
				allowUserToCreateOrganization: false,
				schema: {
					session: { fields: { activeOrganizationId: "active_organization_id" } },
					organization: { fields: { createdAt: "created_at" } },
					member: { fields: { organizationId: "organization_id", userId: "user_id", createdAt: "created_at" } },
					invitation: {
						fields: {
							organizationId: "organization_id",
							expiresAt: "expires_at",
							inviterId: "inviter_id",
							createdAt: "created_at",
						},
					},
				},
			}),
			// Keep last: it writes the cookies Better Auth sets inside a server function.
			tanstackStartCookies(),
		],
	} satisfies BetterAuthOptions;
}

function create() {
	const { databaseUrl, secret, baseUrl } = authSettings();
	const pool = new Pool({ connectionString: databaseUrl });
	return { auth: betterAuth(authOptions(pool, secret, baseUrl)), pool };
}

let instance: ReturnType<typeof create> | undefined;

export function getAuth() {
	instance ??= create();
	return instance.auth;
}

export function getPool(): Pool {
	instance ??= create();
	return instance.pool;
}
