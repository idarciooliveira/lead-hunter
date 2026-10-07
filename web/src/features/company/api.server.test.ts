import { afterEach, describe, expect, it, vi } from "vitest";
import { stubApi } from "#/lib/test-api";
import { fetchCompany, saveCompany } from "./api.server";
import { COMPANY } from "./fixtures";
import type { CompanyProfile } from "./schema";

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
});

describe("company API", () => {
	it("reads the profile and drops what the UI does not edit", async () => {
		stubApi({
			"/company": {
				name: "Mock Software",
				intro: "Fazemos software",
				services: [{ name: "Site", price: null, deliveryTime: "2 semanas" }],
				area: ["Luanda"],
				clients: [],
				cases: [{ sector: "Clínicas", client: null, problem: "x", built: "y", result: "z", mayName: false }],
				entryOffer: null,
				weeklyCapacity: 35,
			},
		});
		await expect(fetchCompany()).resolves.toEqual({
			name: "Mock Software",
			intro: "Fazemos software",
			services: [{ name: "Site", price: null, deliveryTime: "2 semanas" }],
			entryOffer: null,
			area: ["Luanda"],
			clients: [],
			cases: [{ sector: "Clínicas", client: null, problem: "x", built: "y", result: "z", mayName: false }],
		});
	});

	it("is null before company setup", async () => {
		stubApi({});
		await expect(fetchCompany()).resolves.toBeNull();
	});
});

describe("saveCompany", () => {
	it("saves the profile on the fixtures and warns about clients without a phone", async () => {
		const backup = structuredClone(COMPANY);
		try {
			const res = await saveCompany({ ...backup, name: "Nova Lda." });
			expect(res.saved.name).toBe("Nova Lda.");
			expect(res.warnings.join(" ")).toContain("matched by name only");
			await expect(fetchCompany()).resolves.toMatchObject({ name: "Nova Lda." });
		} finally {
			Object.assign(COMPANY, backup);
		}
	});

	it("keeps the fields the UI does not edit when an API is set", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://api:8080/api");
		let method: string | undefined;
		let body: unknown;
		const stored = {
			name: "Mock Software",
			intro: "Fazemos software",
			services: [{ name: "Site", price: "100 Kz", deliveryTime: "2 semanas" }],
			entryOffer: "Site",
			area: ["Luanda"],
			clients: [],
			cases: [
				{
					sector: "Clínicas",
					client: "Clínica Sol",
					problem: "Demora",
					built: "Marcações",
					result: "2x mais consultas",
					mayName: true,
				},
			],
			objections: [],
			weeklyCapacity: 35,
			quarterTarget: null,
		};
		vi.stubGlobal(
			"fetch",
			vi.fn(async (_url: string, init?: { method?: string; body?: string }) => {
				method = init?.method;
				if ((init?.method ?? "GET") === "GET") return new Response(JSON.stringify(stored), { status: 200 });
				body = JSON.parse(init?.body ?? "{}");
				return new Response(JSON.stringify({ saved: { ...stored, ...(body as object) }, warnings: [] }), {
					status: 200,
				});
			}),
		);
		const input: CompanyProfile = {
			name: "Mock Renomeada",
			intro: "Fazemos software",
			services: stored.services,
			entryOffer: "Site",
			area: ["Luanda"],
			clients: [],
			cases: [
				{
					sector: "Clínicas",
					client: "Clínica Sol",
					problem: "Demora",
					built: "Marcações",
					result: "3x mais consultas",
					mayName: true,
				},
			],
		};
		const res = await saveCompany(input);
		expect(method).toBe("PUT");
		expect(body).toMatchObject({ intro: "Fazemos software", entryOffer: "Site", name: "Mock Renomeada" });
		// Objections and capacity are CLI-only, so they survive the merge.
		expect(body).toMatchObject({ weeklyCapacity: 35, objections: [] });
		expect(body).toMatchObject({
			cases: [{ sector: "Clínicas", problem: "Demora", built: "Marcações", result: "3x mais consultas" }],
		});
		expect(res.saved.name).toBe("Mock Renomeada");
	});

	it("creates the profile from the UI fields alone when none is stored", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://api:8080/api");
		let body: unknown;
		vi.stubGlobal(
			"fetch",
			vi.fn(async (_url: string, init?: { method?: string; body?: string }) => {
				if ((init?.method ?? "GET") === "GET") {
					return new Response(JSON.stringify({ message: "no company profile yet" }), { status: 404 });
				}
				body = JSON.parse(init?.body ?? "{}");
				return new Response(JSON.stringify({ saved: body, warnings: [] }), { status: 200 });
			}),
		);
		const input: CompanyProfile = {
			name: "Primeira",
			intro: "Frase",
			entryOffer: "Site",
			services: [{ name: "Site", price: "100 Kz", deliveryTime: null }],
			area: [],
			clients: [],
			cases: [],
		};
		await saveCompany(input);
		expect(body).toEqual(input);
	});
});
