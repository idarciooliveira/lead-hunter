import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { getAuthTables } from "better-auth/db";
import type { Pool } from "pg";
import { describe, expect, it } from "vitest";
import { authOptions } from "./auth.server";

/**
 * Flyway owns the auth tables (V8__auth.sql, ADR 0038) and `auth.server.ts` maps Better Auth's names onto them.
 * An upgrade that adds a field, or a typo in the mapping, would only fail at sign-in. This fails in `./check` instead.
 */
function columnsByTable(): Map<string, Set<string>> {
	const sql = readFileSync(resolve(process.cwd(), "../backend/src/main/resources/db/migration/V8__auth.sql"), "utf8");
	const tables = new Map<string, Set<string>>();
	for (const [, name, body] of sql.matchAll(/create table (\w+) \(([\s\S]*?)\n\);/g)) {
		const columns = new Set<string>();
		for (const line of body.split("\n")) {
			const column = /^\s{4}(\w+)\s+\w/.exec(line)?.[1];
			if (column && column !== "unique") columns.add(column);
		}
		tables.set(name, columns);
	}
	return tables;
}

describe("Better Auth tables", () => {
	const options = authOptions({} as Pool, "secret", "http://localhost:3000");
	const tables = columnsByTable();

	it("reads the tables from V8", () => {
		expect([...tables.keys()].sort()).toEqual(
			["app_user", "auth_account", "auth_session", "auth_verification", "invitation", "member", "organization"].sort(),
		);
	});

	for (const [key, table] of Object.entries(getAuthTables(options))) {
		if (key === "rateLimit") continue;
		it(`${key} maps onto ${table.modelName}`, () => {
			const columns = tables.get(table.modelName);
			expect(columns, `no table ${table.modelName} in V8__auth.sql`).toBeDefined();
			const wanted = ["id", ...Object.entries(table.fields).map(([name, field]) => field.fieldName ?? name)];
			expect(wanted.filter((column) => !columns?.has(column))).toEqual([]);
		});
	}
});
