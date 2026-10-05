import { afterEach, describe, expect, it, vi } from "vitest";
import { stubApi } from "#/lib/test-api";
import { fetchUsage } from "./api";

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
});

describe("usage API", () => {
	it("joins the month's totals and its newest runs and calls", async () => {
		stubApi({
			"/usage?month=2026-10": {
				apify: { costUsd: 0.25 },
				llm: { costUsd: 0.5 },
				byCampaign: [{ slug: "mock-clinicas", apifyUsd: 0.25, llmUsd: 0.5 }],
				totalUsd: 0.75,
				budgetUsd: 10,
			},
			"/usage/entries?month=2026-10&limit=30": [
				{ kind: "llm", at: "2026-10-05T10:02:00Z", campaignSlug: null, label: "gemma, test", costUsd: null },
			],
		});
		await expect(fetchUsage("2026-10")).resolves.toEqual({
			month: "2026-10",
			budgetUsd: 10,
			apifyUsd: 0.25,
			llmUsd: 0.5,
			byCampaign: [{ campaign: "mock-clinicas", usd: 0.75 }],
			events: [{ at: "2026-10-05T10:02:00Z", campaign: null, source: "LLM", detail: "gemma, test", costUsd: null }],
		});
	});
});
