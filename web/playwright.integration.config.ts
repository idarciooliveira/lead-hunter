import { defineConfig, devices } from "@playwright/test";
import {
	E2E_DATABASE_URL,
	E2E_ORG_ID,
	E2E_PASSWORD_FIXTURE,
	E2E_SCHEMA,
	E2E_USER_EMAIL,
	E2E_USER_ID,
} from "./e2e/auth-fixture";

const WEB_PORT = 3320;
const API_PORT = 3330;
const API_TOKEN = "mock-token";
// The web server keeps its auth tables in a throwaway schema that e2e/seed-auth.mjs builds.
const SESSION_DATABASE_URL = `${E2E_DATABASE_URL}${E2E_DATABASE_URL.includes("?") ? "&" : "?"}options=-c%20search_path%3D${E2E_SCHEMA}`;

/**
 * Integration with the HTTP API (docs/api.md). The web server runs with
 * LEADHUNTER_API_URL pointing at the mock API in e2e/mock-api.mjs, which
 * serves the backend contract the MockMvc suite proves for real and checks
 * the service token and the user and organization headers the server
 * functions send (ADR 0037, 0043). The web server signs in against real
 * Postgres tables built from the Flyway migration, so this project needs
 * `docker compose up -d postgres` (or E2E_DATABASE_URL). The `setup` project
 * signs in once and the others reuse the session.
 * The `smoke` project keeps covering the fixture build.
 */
export default defineConfig({
	testDir: "e2e",
	globalTeardown: "./e2e/global-teardown.ts",
	fullyParallel: true,
	forbidOnly: !!process.env.CI,
	reporter: process.env.CI ? "github" : "list",
	use: { baseURL: `http://localhost:${WEB_PORT}`, locale: "pt-PT", timezoneId: "Africa/Luanda" },
	webServer: [
		{
			command: "node e2e/mock-api.mjs",
			env: {
				MOCK_API_PORT: String(API_PORT),
				MOCK_API_TOKEN: API_TOKEN,
				MOCK_API_USER: E2E_USER_ID,
				MOCK_API_ORG: E2E_ORG_ID,
			},
			port: API_PORT,
			reuseExistingServer: !process.env.CI,
			timeout: 60_000,
		},
		{
			command: "node e2e/seed-auth.mjs && pnpm build && pnpm start",
			env: {
				PORT: String(WEB_PORT),
				LEADHUNTER_API_URL: `http://localhost:${API_PORT}/api`,
				LEADHUNTER_API_TOKEN: API_TOKEN,
				DATABASE_URL: SESSION_DATABASE_URL,
				BETTER_AUTH_SECRET: "e2e-secret-e2e-secret-e2e-secret-0001",
				BETTER_AUTH_URL: `http://localhost:${WEB_PORT}`,
				E2E_DATABASE_URL,
				E2E_SCHEMA,
				E2E_USER_ID,
				E2E_ORG_ID,
				E2E_USER_EMAIL,
				E2E_PASSWORD_FIXTURE,
			},
			url: `http://localhost:${WEB_PORT}/entrar`,
			reuseExistingServer: !process.env.CI,
			timeout: 180_000,
		},
	],
	projects: [
		{ name: "setup", testMatch: /auth\.setup\.ts/ },
		{
			name: "integration",
			testMatch: /(api-integration|login)\.spec\.ts/,
			dependencies: ["setup"],
			use: { ...devices["Desktop Chrome"], storageState: "e2e/.auth/state.json" },
		},
	],
});
