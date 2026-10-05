import { expect, test } from "@playwright/test";

/**
 * The `integration` project builds the app with VITE_LEADHUNTER_API_URL
 * pointing at the mock API (e2e/mock-api.mjs), which serves the backend
 * contract from docs/api.md. These specs prove every page renders real HTTP
 * responses instead of fixtures: the mock names appear nowhere else.
 */
test("leads list renders the API leads ranked first", async ({ page }) => {
	const errors: string[] = [];
	page.on("pageerror", (e) => errors.push(e.message));
	await page.goto("/leads");
	await expect(page.getByRole("heading", { level: 1, name: "Leads" })).toBeVisible();
	await expect(page.getByRole("link", { name: "Mock Sorriso" }).first()).toBeVisible();
	await expect(page.getByText("Mock Girassol").first()).toBeVisible();
	expect(errors).toEqual([]);
});

test("lead detail renders the API score breakdown", async ({ page }) => {
	const errors: string[] = [];
	page.on("pageerror", (e) => errors.push(e.message));
	page.on("console", (m) => m.type() === "error" && errors.push(m.text()));
	await page.goto("/leads/9001");
	await expect(page.getByRole("heading", { level: 1, name: "Mock Sorriso" })).toBeVisible();
	await expect(page.getByText("Telefone móvel", { exact: true })).toBeVisible();
	expect(errors).toEqual([]);
});

test("unknown API lead shows the not found page", async ({ page }) => {
	await page.goto("/leads/999999");
	await expect(page.getByText("Página não encontrada")).toBeVisible();
});

test("campaigns, company and usage render from the API", async ({ page }) => {
	const errors: string[] = [];
	page.on("pageerror", (e) => errors.push(e.message));
	await page.goto("/campanhas");
	await expect(page.getByRole("link", { name: "Mock Clínicas" })).toBeVisible();
	await expect(page.getByText("Mock Software, Lda.").first()).toBeVisible();
	await page.goto("/empresa");
	await expect(page.getByText("Mock marcações online")).toBeVisible();
	await page.goto("/uso");
	await expect(page.getByText("Mock Talatona, SUCCEEDED")).toBeVisible();
	expect(errors).toEqual([]);
});

test("unknown API campaign shows the not found page", async ({ page }) => {
	await page.goto("/campanhas/nope");
	await expect(page.getByText("Página não encontrada")).toBeVisible();
});

// These specs change the mock's in-memory leads and runs, so they run serially
// with a reset first. The read-only specs above never depend on that state.
test.describe
	.serial("specs that change the mock", () => {
		test.beforeEach(async ({ request }) => {
			// The mock API port matches playwright.integration.config.ts.
			const port = process.env.MOCK_API_PORT ?? "3330";
			await request.post(`http://localhost:${port}/__reset`);
		});

		test("lead outcome saves through the API and the chip changes", async ({ page }) => {
			const errors: string[] = [];
			page.on("pageerror", (e) => errors.push(e.message));
			await page.goto("/leads/9001");
			await expect(page.getByRole("heading", { level: 1, name: "Mock Sorriso" })).toBeVisible();
			const patched = page.waitForResponse(
				(r) => r.request().method() === "PATCH" && r.url().includes("/api/leads/9001"),
			);
			await page.getByRole("button", { name: "Contactado" }).click();
			const response = await patched;
			expect(response.ok()).toBe(true);
			await expect(page.getByRole("button", { name: "Contactado" })).toHaveAttribute("aria-pressed", "true");
			expect(errors).toEqual([]);
		});

		test("a run starts through the dry run and the over-limit opt-in", async ({ page }) => {
			const errors: string[] = [];
			page.on("pageerror", (e) => errors.push(e.message));
			await page.goto("/campanhas/mock-clinicas");
			await expect(page.getByRole("heading", { level: 1, name: "Mock Clínicas" })).toBeVisible();
			await expect(page.getByText("Mock clínicas privadas", { exact: false })).toBeVisible();
			await expect(page.getByText("Esta campanha ainda não correu.")).toBeVisible();
			await page.getByRole("button", { name: /Executar/ }).click();
			await expect(page.getByText("clínica em Kilamba")).toBeVisible();
			await page.getByRole("checkbox").check();
			const started = page.waitForResponse(
				(r) => r.request().method() === "POST" && r.url().includes("allowOverLimit=true"),
			);
			await page.getByRole("button", { name: /Executar com --allow-over-limit/ }).click();
			expect((await started).status()).toBe(202);
			await expect(page.getByRole("dialog")).toHaveCount(0);
			await expect(page.getByText("Esta campanha ainda não correu.")).toHaveCount(0);
			await expect(page.getByText("Concluída")).toBeVisible();
			expect(errors).toEqual([]);
		});

		test("hoje shows the qualified-NEW mock lead", async ({ page }) => {
			const errors: string[] = [];
			page.on("pageerror", (e) => errors.push(e.message));
			await page.goto("/hoje");
			await expect(page.getByRole("heading", { level: 1, name: "Hoje" })).toBeVisible();
			await expect(page.getByRole("link", { name: "Mock Sorriso" }).first()).toBeVisible();
			await expect(page.getByText("Mock Girassol")).toHaveCount(0);
			expect(errors).toEqual([]);
		});
	});
