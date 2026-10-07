import { test as setup } from "@playwright/test";
import { E2E_USER_EMAIL, e2ePassword } from "./auth-fixture";

export const STATE = "e2e/.auth/state.json";

/** Signs in once through the login page; the integration specs reuse the session cookie. */
setup("sign in", async ({ page }) => {
	await page.goto("/entrar");
	await page.getByLabel("Email").fill(E2E_USER_EMAIL);
	await page.getByLabel("Palavra-passe").fill(e2ePassword());
	await page.getByRole("button", { name: "Entrar" }).click();
	await page.waitForURL("**/hoje");
	await page.context().storageState({ path: STATE });
});
