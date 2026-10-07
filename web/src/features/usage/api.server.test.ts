import { afterEach, describe, expect, it, vi } from "vitest";
import { NotFoundError } from "#/lib/fake-api";
import { stubApi } from "#/lib/test-api";
import { fetchUsage } from "./api.server";

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
	vi.useRealTimers();
});

describe("usage API", () => {
	it("joins the month's totals and its newest runs and calls", async () => {
		vi.useFakeTimers({ toFake: ["Date"] });
		vi.setSystemTime(new Date("2026-11-02T12:00:00Z"));
		stubApi({
			"/usage?month=2026-10": {
				apify: { costUsd: 0.25 },
				llm: { costUsd: 0.5 },
				byCampaign: [
					{ slug: "mock-clinicas", apifyUsd: 0.25, llmUsd: 0.5 },
					{ slug: null, apifyUsd: 0.1, llmUsd: 0 },
				],
				totalUsd: 0.85,
				budgetUsd: 10,
				committedUsd: 8.85,
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
			committedUsd: null,
			byCampaign: [
				{ campaign: "mock-clinicas", usd: 0.75 },
				{ campaign: "Sem campanha ou apagada", usd: 0.1 },
			],
			events: [{ at: "2026-10-05T10:02:00Z", campaign: null, source: "LLM", detail: "gemma, test", costUsd: null }],
		});
	});

	it("passes what is spent or reserved through for this UTC month only", async () => {
		vi.useFakeTimers({ toFake: ["Date"] });
		vi.setSystemTime(new Date("2026-10-07T12:00:00Z"));
		stubApi({
			"/usage?month=2026-10": {
				apify: { costUsd: 0 },
				llm: { costUsd: 0 },
				byCampaign: [],
				totalUsd: 0,
				budgetUsd: 10,
				committedUsd: 8,
			},
			"/usage/entries?month=2026-10&limit=30": [],
		});
		await expect(fetchUsage("2026-10")).resolves.toMatchObject({ committedUsd: 8 });
	});

	it("sends the campaign to both calls when one is chosen", async () => {
		vi.useFakeTimers({ toFake: ["Date"] });
		vi.setSystemTime(new Date("2026-10-07T12:00:00Z"));
		const fetch = stubApi({
			"/usage?month=2026-10&campaign=mock-clinicas": {
				apify: { costUsd: 0.25 },
				llm: { costUsd: 0.5 },
				byCampaign: [{ slug: "mock-clinicas", apifyUsd: 0.25, llmUsd: 0.5 }],
				totalUsd: 0.75,
				budgetUsd: 10,
				committedUsd: 8,
			},
			"/usage/entries?month=2026-10&campaign=mock-clinicas&limit=30": [],
		});
		await expect(fetchUsage("2026-10", "mock-clinicas")).resolves.toMatchObject({
			apifyUsd: 0.25,
			llmUsd: 0.5,
			byCampaign: [{ campaign: "mock-clinicas", usd: 0.75 }],
			committedUsd: null,
		});
		expect(fetch.mock.calls.map(([url]) => url)).toEqual([
			"http://api:8080/api/usage?month=2026-10&campaign=mock-clinicas",
			"http://api:8080/api/usage/entries?month=2026-10&campaign=mock-clinicas&limit=30",
		]);
	});

	it("leaves the campaign out of both calls when none is chosen", async () => {
		const fetch = stubApi({
			"/usage?month=2026-10": {
				apify: { costUsd: 0 },
				llm: { costUsd: 0 },
				byCampaign: [],
				totalUsd: 0,
				budgetUsd: 10,
				committedUsd: 0,
			},
			"/usage/entries?month=2026-10&limit=30": [],
		});
		await fetchUsage("2026-10", undefined);
		expect(fetch.mock.calls.map(([url]) => url)).toEqual([
			"http://api:8080/api/usage?month=2026-10",
			"http://api:8080/api/usage/entries?month=2026-10&limit=30",
		]);
	});

	it("passes an unknown campaign's 404 on as NotFoundError", async () => {
		stubApi({});
		await expect(fetchUsage("2026-10", "nao-existe")).rejects.toBeInstanceOf(NotFoundError);
	});

	it("narrows the fixture month to one campaign when no API is set", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "");
		await expect(fetchUsage("2026-10", "clinicas-talatona")).resolves.toMatchObject({
			month: "2026-10",
			budgetUsd: 10,
			apifyUsd: 0.84,
			llmUsd: 0.28,
			byCampaign: [{ campaign: "Clínicas Talatona", usd: 1.12 }],
		});
		const { events } = await fetchUsage("2026-10", "clinicas-talatona");
		expect(events.map((e) => e.campaign)).toEqual(["Clínicas Talatona", "Clínicas Talatona"]);
	});
});
