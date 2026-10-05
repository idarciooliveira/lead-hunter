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
