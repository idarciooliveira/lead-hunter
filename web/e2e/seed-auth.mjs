// Builds the auth tables in a throwaway schema of the local Postgres (docker compose up -d postgres, or
// E2E_DATABASE_URL), straight from the Flyway migration, and seeds one user the way the CLI would: the password
// hash is the one the Java side wrote (ADR 0042). It runs in the web server's start command, because Better Auth
// checks the tables on its first request. global-teardown.ts drops the schema, so the database's own tables are
// never touched. The ids and the schema name come from the Playwright config.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import pg from "pg";

const { E2E_DATABASE_URL, E2E_SCHEMA, E2E_USER_ID, E2E_ORG_ID, E2E_USER_EMAIL, E2E_PASSWORD_FIXTURE } = process.env;
const backend = resolve(process.cwd(), "..", "backend");
const migration = readFileSync(resolve(backend, "src/main/resources/db/migration/V8__auth.sql"), "utf8");
const fixture = readFileSync(resolve(backend, "src/test/resources/auth", E2E_PASSWORD_FIXTURE), "utf8");
const hash = /^hash=(.+)$/m.exec(fixture)?.[1].trim();
if (!hash) throw new Error(`no hash in ${E2E_PASSWORD_FIXTURE}`);

const client = new pg.Client({ connectionString: E2E_DATABASE_URL });
await client.connect();
try {
	await client.query(`drop schema if exists ${E2E_SCHEMA} cascade; create schema ${E2E_SCHEMA}`);
	await client.query(`set search_path to ${E2E_SCHEMA}`);
	await client.query(migration);
	await client.query("insert into organization (id, name, slug) values ($1, 'Clínica E2E', 'clinica-e2e')", [
		E2E_ORG_ID,
	]);
	await client.query("insert into app_user (id, name, email, email_verified) values ($1, 'Ana E2E', $2, true)", [
		E2E_USER_ID,
		E2E_USER_EMAIL,
	]);
	await client.query(
		"insert into auth_account (id, user_id, account_id, provider_id, password) values ('e2e-account', $1, $1, 'credential', $2)",
		[E2E_USER_ID, hash],
	);
	await client.query("insert into member (id, organization_id, user_id, role) values ('e2e-member', $1, $2, 'owner')", [
		E2E_ORG_ID,
		E2E_USER_ID,
	]);
} finally {
	await client.end();
}
