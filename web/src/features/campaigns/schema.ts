import { z } from "zod";

export const CampaignState = z.enum(["DRAFT", "RUNNING", "ENRICHING", "DONE", "FAILED"]);
export type CampaignState = z.infer<typeof CampaignState>;

/** Counts from the last run, in pipeline order (ADR 0006, 0027). */
export const Funnel = z.object({
	scraped: z.number().int(),
	scrapeCostUsd: z.number(),
	kept: z.number().int(),
	filterNote: z.string(),
	cutShare: z.number(),
	qualified: z.number().int(),
	enriched: z.number().int(),
	enrichCostUsd: z.number(),
	enriching: z.boolean(),
});
export type Funnel = z.infer<typeof Funnel>;

export const Campaign = z.object({
	slug: z.string(),
	name: z.string(),
	sector: z.string(),
	service: z.string(),
	locations: z.array(z.string()),
	state: CampaignState,
	qualifiedCount: z.number().int(),
	/** Apify and LLM spend so far. */
	spendUsd: z.number(),
	/** Null until the API serves a funnel report. */
	funnel: Funnel.nullable(),
});
export type Campaign = z.infer<typeof Campaign>;
export const CampaignList = z.array(Campaign);

/**
 * The POST /api/campaigns body (docs/api.md Writes): the wizard answers plus
 * the concrete search. Fields the wizard never asks for take the file
 * defaults, so they are left out.
 */
export const CampaignWriteBody = z.object({
	name: z.string(),
	answers: z.object({ sector: z.string(), problem: z.string(), service: z.string() }),
	search: z.object({
		terms: z.array(z.string()),
		locations: z.array(z.string()),
		minReviews: z.number().int(),
	}),
});
export type CampaignWriteBody = z.infer<typeof CampaignWriteBody>;

/** Creating a campaign adds the slug, like `campaign create -f`. */
export const CampaignCreateInput = CampaignWriteBody.extend({ slug: z.string() });
export type CampaignCreateInput = z.infer<typeof CampaignCreateInput>;
