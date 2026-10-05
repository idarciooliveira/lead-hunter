import { expect, test } from "@playwright/test";

/**
 * The `integration` project runs the web server with LEADHUNTER_API_URL
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

test("the browser never calls the API, only the web server does (ADR 0037)", async ({ page }) => {
	const direct: string[] = [];
	page.on("request", (r) => r.url().includes(":3330/") && direct.push(r.url()));
	await page.goto("/campanhas");
	await page.getByRole("link", { name: "Mock Clínicas" }).click();
	await expect(page.getByRole("heading", { level: 1, name: "Mock Clínicas" })).toBeVisible();
	await page.goto("/leads/9001");
	await expect(page.getByRole("heading", { level: 1, name: "Mock Sorriso" })).toBeVisible();
	expect(direct).toEqual([]);
});

test("a backend error reaches the page with its message", async ({ page }) => {
	await page.goto("/campanhas/mock-clinicas");
	await page.locator("html[data-hydrated]").waitFor();
	await page.getByRole("button", { name: /Enriquecer/ }).click();
	await expect(page.getByRole("alert")).toHaveText(
		"nothing to enrich: every qualified lead of mock-clinicas is enriched",
	);
});

test("unknown API campaign shows the not found page", async ({ page }) => {
	await page.goto("/campanhas/nope");
	await expect(page.getByText("Página não encontrada")).toBeVisible();
});

// The mock API and its token match playwright.integration.config.ts.
const MOCK = "http://localhost:3330";
const AUTH = { authorization: "Bearer mock-token" };

// These specs change the mock's in-memory leads and runs, so they run serially
// with a reset first. The read-only specs above never depend on that state.
// The browser only talks to the web server, so they check the change on the mock itself.
test.describe
	.serial("specs that change the mock", () => {
		test.beforeEach(async ({ request }) => {
			await request.post(`${MOCK}/__reset`);
		});

		test("lead outcome saves through the API and the chip changes", async ({ page, request }) => {
			const errors: string[] = [];
			page.on("pageerror", (e) => errors.push(e.message));
			await page.goto("/leads/9001");
			await expect(page.getByRole("heading", { level: 1, name: "Mock Sorriso" })).toBeVisible();
			await page.getByRole("button", { name: "Contactado" }).click();
			await expect(page.getByRole("button", { name: "Contactado" })).toHaveAttribute("aria-pressed", "true");
			const lead = await (await request.get(`${MOCK}/api/leads/9001`, { headers: AUTH })).json();
			expect(lead.status).toBe("CONTACTED");
			expect(errors).toEqual([]);
		});

		test("a run starts through the dry run and the over-limit opt-in", async ({ page, request }) => {
			const errors: string[] = [];
			page.on("pageerror", (e) => errors.push(e.message));
			await page.goto("/campanhas/mock-clinicas");
			await expect(page.getByRole("heading", { level: 1, name: "Mock Clínicas" })).toBeVisible();
			await expect(page.getByText("Mock clínicas privadas", { exact: false })).toBeVisible();
			await expect(page.getByText("Esta campanha ainda não correu.")).toBeVisible();
			await page.getByRole("button", { name: /Executar/ }).click();
			await expect(page.getByText("clínica em Kilamba")).toBeVisible();
			await page.getByRole("checkbox").check();
			await page.getByRole("button", { name: /Executar com --allow-over-limit/ }).click();
			await expect(page.getByRole("dialog")).toHaveCount(0);
			// The mock only starts a run with allowOverLimit=true, so one run proves the opt-in reached it.
			const runs = await (await request.get(`${MOCK}/api/campaigns/mock-clinicas/runs`, { headers: AUTH })).json();
			expect(runs).toHaveLength(1);
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
