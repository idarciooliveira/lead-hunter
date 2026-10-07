import { describe, expect, it } from "vitest";
import { safeNext } from "./next";

describe("safeNext", () => {
	it("keeps a path on this site, with its query", () => {
		expect(safeNext("/leads?stage=QUALIFIED")).toBe("/leads?stage=QUALIFIED");
	});

	it("falls back to Hoje when there is none", () => {
		expect(safeNext(undefined)).toBe("/hoje");
		expect(safeNext("")).toBe("/hoje");
	});

	it("refuses anything that leaves the site", () => {
		expect(safeNext("https://evil.example")).toBe("/hoje");
		expect(safeNext("//evil.example")).toBe("/hoje");
		expect(safeNext("/\\evil.example")).toBe("/hoje");
		expect(safeNext("javascript:alert(1)")).toBe("/hoje");
	});
});
