import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { hashPassword, verifyPassword } from "better-auth/crypto";
import { describe, expect, it } from "vitest";

/**
 * The CLI writes password hashes for Better Auth (ADR 0042). The backend's `PasswordHasherTest` pins the Java
 * side to these two fixtures; these tests pin the installed Better Auth to the same files, so an upgrade that
 * changes the format fails here before a CLI-made account stops signing in.
 */
function fixture(name: string): Record<string, string> {
	const text = readFileSync(resolve(process.cwd(), "../backend/src/test/resources/auth", name), "utf8");
	const values: Record<string, string> = {};
	for (const line of text.split("\n")) {
		const at = line.indexOf("=");
		if (line.startsWith("#") || at < 0) continue;
		values[line.slice(0, at)] = line.slice(at + 1).trim();
	}
	return values;
}

describe("password hashes shared with the backend", () => {
	it("accepts the hash Java writes", async () => {
		const { password, hash } = fixture("java-hash.properties");
		expect(await verifyPassword({ hash, password })).toBe(true);
		expect(await verifyPassword({ hash, password: "wrong password" })).toBe(false);
	});

	it("still accepts the hash its own version once wrote", async () => {
		const { password, hash } = fixture("better-auth-hash.properties");
		expect(await verifyPassword({ hash, password })).toBe(true);
	});

	it("writes the same shape Java reads", async () => {
		expect(await hashPassword("a long enough password")).toMatch(/^[0-9a-f]{32}:[0-9a-f]{128}$/);
	});
});
