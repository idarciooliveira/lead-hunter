import type { Run, ScrapePlan } from "./schema";

export const RUNS: Run[] = [
	{
		id: "run_0234",
		campaignSlug: "clinicas-talatona",
		kind: "ENRICH",
		status: "RUNNING",
		startedAt: "2026-10-05T10:02:00+01:00",
		done: 14,
		total: 38,
		estimated: false,
		costUsd: 0.28,
		error: null,
	},
	{
		id: "run_0231",
		campaignSlug: "clinicas-talatona",
		kind: "SCRAPE",
		status: "DONE",
		startedAt: "2026-10-05T09:41:00+01:00",
		done: 142,
		total: null,
		estimated: false,
		costUsd: 0.84,
		error: null,
	},
	{
		id: "run_0230",
		campaignSlug: "clinicas-talatona",
		kind: "SCRAPE",
		status: "FAILED",
		startedAt: "2026-10-04T17:12:00+01:00",
		done: 0,
		total: null,
		estimated: false,
		costUsd: 0,
		error: "timeout do actor após 15 min",
	},
	{
		id: "run_0229",
		campaignSlug: "clinicas-talatona",
		kind: "DRY_RUN",
		status: "DONE",
		startedAt: "2026-10-04T17:05:00+01:00",
		done: 140,
		total: null,
		estimated: true,
		costUsd: 0,
		error: null,
	},
	{
		id: "run_0221",
		campaignSlug: "restaurantes-maianga",
		kind: "SCRAPE",
		status: "DONE",
		startedAt: "2026-09-28T10:12:00+01:00",
		done: 410,
		total: null,
		estimated: false,
		costUsd: 1.64,
		error: null,
	},
	{
		id: "run_0215",
		campaignSlug: "escolas-viana",
		kind: "SCRAPE",
		status: "DONE",
		startedAt: "2026-10-01T14:30:00+01:00",
		done: 118,
		total: null,
		estimated: false,
		costUsd: 0.78,
		error: null,
	},
	{
		id: "run_0226",
		campaignSlug: "oficinas-zango",
		kind: "SCRAPE",
		status: "FAILED",
		startedAt: "2026-10-02T11:05:00+01:00",
		done: 0,
		total: null,
		estimated: false,
		costUsd: 0,
		error: "timeout do actor após 15 min",
	},
];

/**
 * What the fake API answers for a dry run: one search per location, 80 places
 * each, so a two-location campaign passes the 150-place limit of one run.
 */
export function fixtureScrapePlan(locations: string[], sector: string): ScrapePlan {
	const requests = locations.map((location) => ({ location, terms: [sector.toLowerCase()], maxPlaces: 80 }));
	const maxPlaces = requests.length * 80;
	return { requests, maxPlaces, estimatedMaxUsd: maxPlaces * 0.004, overLimit: maxPlaces > 150 };
}

export const FIXTURE_ENRICH_BATCH = 20;
