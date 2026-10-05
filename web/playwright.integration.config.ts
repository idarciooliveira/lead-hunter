import { defineConfig, devices } from "@playwright/test";

const WEB_PORT = 3320;
const API_PORT = 3330;
const API_TOKEN = "mock-token";

/**
 * Integration with the HTTP API (docs/api.md). The web server runs with
 * LEADHUNTER_API_URL pointing at the mock API in e2e/mock-api.mjs, which
 * serves the backend contract the MockMvc suite proves for real and checks
 * the service token the server functions send (ADR 0037).
 * The `smoke` project keeps covering the fixture build.
 */
export default defineConfig({
	testDir: "e2e",
	testMatch: /api-integration\.spec\.ts/,
	fullyParallel: true,
	forbidOnly: !!process.env.CI,
	reporter: process.env.CI ? "github" : "list",
	use: { baseURL: `http://localhost:${WEB_PORT}`, locale: "pt-PT", timezoneId: "Africa/Luanda" },
	webServer: [
		{
			command: "node e2e/mock-api.mjs",
			env: { MOCK_API_PORT: String(API_PORT), MOCK_API_TOKEN: API_TOKEN },
			port: API_PORT,
			reuseExistingServer: !process.env.CI,
			timeout: 60_000,
		},
		{
			command: "pnpm build && pnpm start",
			env: {
				PORT: String(WEB_PORT),
				LEADHUNTER_API_URL: `http://localhost:${API_PORT}/api`,
				LEADHUNTER_API_TOKEN: API_TOKEN,
			},
			url: `http://localhost:${WEB_PORT}/hoje`,
			reuseExistingServer: !process.env.CI,
			timeout: 180_000,
		},
	],
	projects: [{ name: "integration", use: { ...devices["Desktop Chrome"] } }],
});
