import { test } from "@playwright/test";
import { PAGES } from "./pages";

const OUT = "../docs/screenshots/web";

test.describe("desktop", () => {
	test.use({ viewport: { width: 1440, height: 1000 } });
	for (const page of PAGES) {
		test(page.name, async ({ page: p }) => {
			await p.goto(page.path);
			await p.getByRole("heading", { level: 1, name: page.heading }).waitFor();
			await p.screenshot({ path: `${OUT}/desktop-${page.name}.png`, fullPage: true });
		});
	}

	test("dark", async ({ page }) => {
		await page.goto("/leads/l1");
		await page.locator("html[data-hydrated]").waitFor();
		await page.getByRole("button", { name: "Alternar tema claro e escuro" }).first().click();
		await page.screenshot({ path: `${OUT}/desktop-lead-dark.png`, fullPage: true });
	});

	test("dry-run dialog", async ({ page }) => {
		await page.goto("/campanhas/clinicas-talatona");
		await page.locator("html[data-hydrated]").waitFor();
		await page.getByRole("button", { name: /Dry run/ }).click();
		await page.getByRole("dialog").waitFor();
		await page.screenshot({ path: `${OUT}/desktop-dry-run.png` });
	});

	test("command palette", async ({ page }) => {
		await page.goto("/hoje");
		await page.locator("html[data-hydrated]").waitFor();
		await page.keyboard.press("Control+k");
		await page.getByRole("dialog").waitFor();
		await page.screenshot({ path: `${OUT}/desktop-palette.png` });
	});
});

test.describe("mobile", () => {
	test.use({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 2 });
	for (const page of PAGES.filter((p) => ["hoje", "leads", "lead"].includes(p.name))) {
		test(page.name, async ({ page: p }) => {
			await p.goto(page.path);
			await p.getByRole("heading", { level: 1, name: page.heading }).waitFor();
			await p.screenshot({ path: `${OUT}/mobile-${page.name}.png` });
		});
	}
});
