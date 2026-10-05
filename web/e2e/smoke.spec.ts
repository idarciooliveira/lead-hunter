import { expect, test } from "@playwright/test";
import { PAGES } from "./pages";

for (const page of PAGES) {
	test(`${page.name} renders without errors`, async ({ page: p }) => {
		const errors: string[] = [];
		p.on("pageerror", (e) => errors.push(e.message));
		p.on("console", (m) => m.type() === "error" && errors.push(m.text()));
		await p.goto(page.path);
		await expect(p.getByRole("heading", { level: 1, name: page.heading })).toBeVisible();
		expect(errors).toEqual([]);
	});
}

test("root redirects to Hoje", async ({ page }) => {
	await page.goto("/");
	await expect(page).toHaveURL(/\/hoje$/);
});

test("unknown lead shows the not found page", async ({ page }) => {
	await page.goto("/leads/nope");
	await expect(page.getByText("Página não encontrada")).toBeVisible();
});

test("sidebar navigates between sections", async ({ page }) => {
	await page.goto("/hoje");
	await page.locator("html[data-hydrated]").waitFor();
	await page
		.getByRole("navigation", { name: "Principal" })
		.first()
		.getByRole("link", { name: /Campanhas/ })
		.click();
	await expect(page.getByRole("heading", { level: 1, name: "Campanhas" })).toBeVisible();
});

test("stage filter narrows the leads and is kept in the URL", async ({ page }) => {
	await page.goto("/leads");
	await page.locator("html[data-hydrated]").waitFor();
	await page.getByRole("button", { name: /Excluídos/ }).click();
	await expect(page).toHaveURL(/stage=EXCLUDED/);
	await expect(page.locator("tbody tr")).toHaveCount(2);
});

test("command palette opens with Ctrl+K and navigates", async ({ page }) => {
	await page.goto("/hoje");
	await page.locator("html[data-hydrated]").waitFor();
	await page.keyboard.press("Control+k");
	await page.getByRole("combobox", { name: "Paleta de comandos" }).fill("uso");
	await page.keyboard.press("Enter");
	await expect(page.getByRole("heading", { level: 1, name: "Uso e custos" })).toBeVisible();
});

test("dry run warns before passing the campaign limit", async ({ page }) => {
	await page.goto("/campanhas/clinicas-talatona");
	await page.locator("html[data-hydrated]").waitFor();
	await page.getByRole("button", { name: /Dry run/ }).click();
	await expect(page.getByText("Esta execução passa o limite da campanha em $0.26.")).toBeVisible();
	await page.getByRole("button", { name: /Continuar/ }).click();
	const run = page.getByRole("button", { name: /Executar com --allow-over-limit/ });
	await expect(run).toBeDisabled();
	await page.getByRole("checkbox").check();
	await expect(run).toBeEnabled();
});

test("wizard walks the questions and updates the YAML", async ({ page }) => {
	await page.goto("/campanhas/nova");
	await page.locator("html[data-hydrated]").waitFor();
	await page.getByRole("textbox").fill("Ginásios Talatona");
	await expect(page.getByText('"Ginásios Talatona"')).toBeVisible();
	await page.getByRole("button", { name: /Seguinte/ }).click();
	await expect(page.getByText("Pergunta 2 de 11")).toBeVisible();
});

test("theme toggle switches to dark", async ({ page }) => {
	await page.goto("/hoje");
	await page.locator("html[data-hydrated]").waitFor();
	await page.getByRole("button", { name: "Alternar tema claro e escuro" }).first().click();
	await expect(page.locator("html")).toHaveClass(/dark/);
});
