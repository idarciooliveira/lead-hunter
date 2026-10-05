import { afterEach, describe, expect, it, vi } from "vitest";
import { stubApi } from "#/lib/test-api";
import { fetchCampaignRuns, previewScrape, startScrape } from "./api.server";

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
});

const JOB = {
	id: 42,
	campaignSlug: "mock-clinicas",
	kind: "DRY_RUN",
	status: "DONE",
	startedAt: "2026-10-05T10:02:00Z",
	done: 80,
	total: null,
	costUsd: 0,
	error: null,
};

describe("runs API", () => {
	it("reads the job history and marks dry runs as estimates", async () => {
		stubApi({ "/campaigns/mock-clinicas/runs": [JOB] });
		await expect(fetchCampaignRuns("mock-clinicas")).resolves.toEqual([{ ...JOB, id: "42", estimated: true }]);
	});

	it("posts the dry run and the start with the opt-in", async () => {
		const plan = { requests: [], maxPlaces: 80, estimatedMaxUsd: 0.32, overLimit: false, dryRunId: 41 };
		const fetch = stubApi({
			"/campaigns/mock-clinicas/runs?dryRun=true": plan,
			"/campaigns/mock-clinicas/runs?allowOverLimit=true": { ...JOB, kind: "SCRAPE", status: "RUNNING" },
		});
		await expect(previewScrape("mock-clinicas")).resolves.toEqual({
			requests: [],
			maxPlaces: 80,
			estimatedMaxUsd: 0.32,
			overLimit: false,
		});
		await expect(startScrape("mock-clinicas", true)).resolves.toMatchObject({ id: "42", status: "RUNNING" });
		expect(fetch.mock.calls.map(([, init]) => init?.method)).toEqual(["POST", "POST"]);
	});
});
