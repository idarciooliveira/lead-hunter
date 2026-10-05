import { afterEach, describe, expect, it, vi } from "vitest";
import type { BackendRun } from "#/lib/api-contract";
import { BACKEND_CAMPAIGN, stubApi } from "#/lib/test-api";
import { fetchCampaign, fetchCampaigns, toCampaign } from "./api.server";

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
