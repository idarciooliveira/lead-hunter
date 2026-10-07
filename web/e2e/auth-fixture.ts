import { readFileSync } from "node:fs";
import { resolve } from "node:path";

/** The account `seed-auth.mjs` creates and the specs sign in with. The Playwright config hands these to the seed. */
export const E2E_DATABASE_URL =
	process.env.E2E_DATABASE_URL ?? "postgresql://leadhunter:leadhunter@localhost:5432/leadhunter";
export const E2E_SCHEMA = "e2e_auth";
export const E2E_USER_ID = "e2e-user";
export const E2E_ORG_ID = "e2e-org";
export const E2E_USER_EMAIL = "ana@e2e.example";
export const E2E_PASSWORD_FIXTURE = "java-hash.properties";

/** The password whose hash the Java side wrote, so the specs sign in with what a CLI-made account would have. */
export function e2ePassword(): string {
	const file = resolve(process.cwd(), "..", "backend/src/test/resources/auth", E2E_PASSWORD_FIXTURE);
	const line = readFileSync(file, "utf8")
		.split("\n")
		.find((l) => l.startsWith("password="));
	if (!line) throw new Error(`no password in ${file}`);
	return line.slice("password=".length).trim();
}
