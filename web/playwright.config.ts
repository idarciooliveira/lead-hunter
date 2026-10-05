import { defineConfig, devices } from "@playwright/test";

const PORT = 3310;

/**
 * `smoke` checks every page renders against the production build. `screenshots`
 * writes the images in docs/screenshots/web, run it after a visual change.
 */
export default defineConfig({
	testDir: "e2e",
	fullyParallel: true,
	forbidOnly: !!process.env.CI,
	reporter: process.env.CI ? "github" : "list",
	use: { baseURL: `http://localhost:${PORT}`, locale: "pt-PT", timezoneId: "Africa/Luanda" },
	webServer: {
		command: "pnpm build && pnpm start",
		env: { PORT: String(PORT) },
		url: `http://localhost:${PORT}/hoje`,
		reuseExistingServer: !process.env.CI,
		timeout: 180_000,
	},
	projects: [
		{ name: "smoke", testMatch: /smoke\.spec\.ts/, use: { ...devices["Desktop Chrome"] } },
		{ name: "screenshots", testMatch: /screenshots\.spec\.ts/, use: { ...devices["Desktop Chrome"] } },
	],
});
