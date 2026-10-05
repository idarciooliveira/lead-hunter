import { z } from "zod";

export const RunKind = z.enum(["SCRAPE", "ENRICH", "DRY_RUN"]);
export const RunStatus = z.enum(["RUNNING", "DONE", "FAILED"]);

/** A row of `campaign_run`, as GET /api/runs/{id} will return it (ADR 0033). */
export const Run = z.object({
	id: z.string(),
	campaignSlug: z.string(),
	kind: RunKind,
	status: RunStatus,
	startedAt: z.iso.datetime({ offset: true }),
	/** Places scraped, or leads enriched so far. */
	done: z.number().int(),
	/** Known total for enrichment; null for scrapes. */
	total: z.number().int().nullable(),
	/** True when `done` is a dry-run estimate. */
	estimated: z.boolean(),
	/** Null when a finished part of the run has no known cost. */
	costUsd: z.number().nullable(),
	error: z.string().nullable(),
});
export type Run = z.infer<typeof Run>;
export const RunList = z.array(Run);

/** The free estimate before a scrape (ADR 0033): what will be searched, at most how many places, and their cost. */
export const ScrapePlan = z.object({
	requests: z.array(z.object({ location: z.string(), terms: z.array(z.string()), maxPlaces: z.number().int() })),
	maxPlaces: z.number().int(),
	estimatedMaxUsd: z.number(),
	/** True when the run could return more places than one run allows; starting it needs an explicit opt-in. */
	overLimit: z.boolean(),
});
export type ScrapePlan = z.infer<typeof ScrapePlan>;

/** The free estimate before an enrichment: qualified leads waiting, and how many this batch takes. */
export const EnrichPlan = z.object({
	pending: z.number().int(),
	batch: z.number().int(),
	maxReviews: z.number().int(),
});
export type EnrichPlan = z.infer<typeof EnrichPlan>;
