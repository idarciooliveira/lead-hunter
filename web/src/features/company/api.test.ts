import { afterEach, describe, expect, it, vi } from "vitest";
import { stubApi } from "#/lib/test-api";
import { fetchCompany } from "./api";

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
});

describe("company API", () => {
	it("reads the profile and drops what the UI does not show", async () => {
		stubApi({
			"/company": {
				name: "Mock Software",
				intro: "Fazemos software",
				services: [{ name: "Site", price: null, deliveryTime: "2 semanas" }],
				area: ["Luanda"],
				clients: [],
				cases: [{ sector: "Clínicas", client: null, problem: "x", built: "y", result: "z", mayName: false }],
				weeklyCapacity: 35,
			},
		});
		await expect(fetchCompany()).resolves.toEqual({
			name: "Mock Software",
			services: [{ name: "Site", price: null, deliveryTime: "2 semanas" }],
			area: ["Luanda"],
			clients: [],
			cases: [{ sector: "Clínicas", client: null, result: "z", mayName: false }],
		});
	});

	it("is null before company setup", async () => {
		stubApi({});
		await expect(fetchCompany()).resolves.toBeNull();
	});
});
