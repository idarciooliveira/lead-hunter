import { expect, test } from "@playwright/test";
import { E2E_USER_EMAIL, e2ePassword } from "./auth-fixture";

// These specs start signed out; the session the other specs share is not used here.
test.use({ storageState: { cookies: [], origins: [] } });

test("a signed-out page request goes to the login page and remembers the page", async ({ page }) => {
	await page.goto("/leads?stage=QUALIFIED");
	await expect(page).toHaveURL(/\/entrar\?next=%2Fleads%3Fstage%3DQUALIFIED$/);
	await expect(page.getByRole("button", { name: "Entrar" })).toBeVisible();
});

test("a signed-out server function call gets a 401, not a redirect", async ({ request }) => {
	// Browsers always send Sec-Fetch-Site; without it the CSRF middleware refuses the call first.
	const res = await request.get("/_serverFn/nope", { maxRedirects: 0, headers: { "sec-fetch-site": "same-origin" } });
	expect(res.status()).toBe(401);
});

test("a cross-site server function call is refused before the session check", async ({ request }) => {
	const res = await request.get("/_serverFn/nope", { maxRedirects: 0, headers: { "sec-fetch-site": "cross-site" } });
	expect(res.status()).toBe(403);
});

test("a wrong password shows the error and stays on the login page", async ({ page }) => {
	await page.goto("/entrar");
	await page.getByLabel("Email").fill(E2E_USER_EMAIL);
	await page.getByLabel("Palavra-passe").fill("not the password");
	await page.getByRole("button", { name: "Entrar" }).click();
	await expect(page.getByText("Invalid email or password")).toBeVisible();
	await expect(page).toHaveURL(/\/entrar/);
});

test("signing in opens the remembered page, shows who is in and signing out locks it again", async ({ page }) => {
	await page.goto("/leads");
	await page.getByLabel("Email").fill(E2E_USER_EMAIL);
	await page.getByLabel("Palavra-passe").fill(e2ePassword());
	await page.getByRole("button", { name: "Entrar" }).click();
	await expect(page).toHaveURL(/\/leads$/);
	// The mock API only answers calls that carry this user and organization (ADR 0043).
	await expect(page.getByRole("link", { name: "Mock Sorriso" }).first()).toBeVisible();
	await expect(page.getByText("Ana E2E")).toBeVisible();
	await expect(page.getByText("Clínica E2E")).toBeVisible();

	await page.locator("html[data-hydrated]").waitFor();
	await page.getByRole("button", { name: "Sair" }).click();
	await expect(page).toHaveURL(/\/entrar/);
	await page.goto("/hoje");
	await expect(page).toHaveURL(/\/entrar\?next=%2Fhoje$/);
});
