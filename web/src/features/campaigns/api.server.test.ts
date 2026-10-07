import { afterEach, describe, expect, it, vi } from "vitest";
import type { BackendRun } from "#/lib/api-contract";
import { BACKEND_CAMPAIGN, stubApi } from "#/lib/test-api";
import { createCampaign, fetchCampaign, fetchCampaigns, toCampaign } from "./api.server";
import { CAMPAIGNS } from "./fixtures";
import type { CampaignCreateInput } from "./schema";

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
});

const RUN: BackendRun = {
	id: 7,
	campaignSlug: "mock-clinicas",
	kind: "ENRICH",
	status: "RUNNING",
	startedAt: "2026-10-05T10:02:00Z",
	done: 3,
	total: 10,
	costUsd: 0.01,
	error: null,
};

describe("backend campaign mapping", () => {
	it("reads sector, service, locations and spend from the campaign", () => {
		expect(toCampaign(BACKEND_CAMPAIGN)).toEqual({
			slug: "mock-clinicas",
			name: "Mock Clínicas",
			sector: "Clínicas",
			service: "Site",
			locations: ["Talatona"],
			state: "DRAFT",
			qualifiedCount: 1,
			spendUsd: 0.21,
			funnel: null,
		});
	});

	it("maps the funnel counts and words the filter note", () => {
		const funnel = {
			scraped: 12,
			scrapeCostUsd: 0.2,
			kept: 9,
			excluded: 3,
			excludedBy: [
				{ reason: "Sem telefone", leads: 2 },
				{ reason: "Cliente", leads: 1 },
			],
			cutShare: 0.4,
			qualified: 4,
			enriched: 2,
			enrichCostUsd: 0.01,
			enriching: true,
		};
		expect(toCampaign({ ...BACKEND_CAMPAIGN, funnel }).funnel).toEqual({
			scraped: 12,
			scrapeCostUsd: 0.2,
			kept: 9,
			filterNote: "3 fora: Sem telefone, Cliente",
			cutShare: 0.4,
			qualified: 4,
			enriched: 2,
			enrichCostUsd: 0.01,
			enriching: true,
		});
		const none = toCampaign({ ...BACKEND_CAMPAIGN, funnel: { ...funnel, excluded: 0, excludedBy: [] } });
		expect(none.funnel?.filterNote).toBe("nenhum lugar excluído");
	});

	it("takes the state from the newest real job", () => {
		const state = (latestRun: BackendRun | null) => toCampaign({ ...BACKEND_CAMPAIGN, latestRun }).state;
		expect(state(null)).toBe("DRAFT");
		expect(state(RUN)).toBe("ENRICHING");
		expect(state({ ...RUN, kind: "SCRAPE" })).toBe("RUNNING");
		expect(state({ ...RUN, status: "FAILED" })).toBe("FAILED");
		expect(state({ ...RUN, status: "DONE" })).toBe("DONE");
	});
});

describe("campaign API", () => {
	it("lists and shows campaigns from the API", async () => {
		stubApi({ "/campaigns": [BACKEND_CAMPAIGN], "/campaigns/mock-clinicas": BACKEND_CAMPAIGN });
		await expect(fetchCampaigns()).resolves.toMatchObject([{ slug: "mock-clinicas" }]);
		await expect(fetchCampaign("mock-clinicas")).resolves.toMatchObject({ name: "Mock Clínicas" });
	});
});

const NEW_CAMPAIGN: CampaignCreateInput = {
	slug: "nova-teste",
	name: "Nova teste",
	answers: { sector: "Clínicas", problem: "Demora", service: "Site" },
	search: { terms: ["clínica"], locations: ["Talatona"], minReviews: 0 },
};

describe("createCampaign", () => {
	it("creates a campaign on the fixtures without an API", async () => {
		const created = await createCampaign(NEW_CAMPAIGN);
		try {
			expect(created.saved).toMatchObject({ slug: "nova-teste", state: "DRAFT", qualifiedCount: 0 });
			expect(created.warnings).toEqual([]);
			await expect(fetchCampaign("nova-teste")).resolves.toMatchObject({ name: "Nova teste" });
		} finally {
			const index = CAMPAIGNS.findIndex((c) => c.slug === "nova-teste");
			if (index >= 0) CAMPAIGNS.splice(index, 1);
		}
	});

	it("refuses a slug the fixtures already have", async () => {
		await expect(createCampaign({ ...NEW_CAMPAIGN, slug: "clinicas-talatona" })).rejects.toThrow(
			"campaign 'clinicas-talatona' already exists",
		);
	});

	it("POSTs the campaign file over HTTP", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://api:8080/api");
		let method: string | undefined;
		let url: string | undefined;
		let body: unknown;
		vi.stubGlobal(
			"fetch",
			vi.fn(async (requestUrl: string, init?: { method?: string; body?: string }) => {
				method = init?.method;
				url = requestUrl;
				body = JSON.parse(init?.body ?? "{}");
				return new Response(JSON.stringify({ saved: BACKEND_CAMPAIGN, warnings: ["w1"] }), { status: 201 });
			}),
		);
		const created = await createCampaign(NEW_CAMPAIGN);
		expect(method).toBe("POST");
		expect(url).toBe("http://api:8080/api/campaigns");
		expect(body).toEqual(NEW_CAMPAIGN);
		expect(created).toMatchObject({ warnings: ["w1"], saved: { slug: "mock-clinicas" } });
	});
});
