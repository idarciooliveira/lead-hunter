import { z } from "zod";

/**
 * The backend shapes `docs/api.md` serves. Only the fields the UI reads are
 * listed; Zod strips the rest. As the API grows (pitches, audits, funnels),
 * the mappers in each feature's `api.server.ts` pick the new fields up here.
 */

/** One job of GET /api/campaigns/{slug}/runs and GET /api/runs/{id} (ADR 0033). */
export const BackendRun = z.object({
	id: z.number().int(),
	campaignSlug: z.string(),
	kind: z.enum(["SCRAPE", "ENRICH", "DRY_RUN"]),
	status: z.enum(["RUNNING", "DONE", "FAILED"]),
	startedAt: z.iso.datetime({ offset: true }),
	done: z.number().int(),
	total: z.number().int().nullable(),
	costUsd: z.number().nullable(),
	error: z.string().nullable(),
});
export type BackendRun = z.infer<typeof BackendRun>;
export const BackendRunList = z.array(BackendRun);

/** One row of GET /api/campaigns and GET /api/campaigns/{slug}. */
export const BackendCampaign = z.object({
	slug: z.string(),
	name: z.string(),
	answers: z.object({ sector: z.string().nullable(), service: z.string().nullable() }),
	search: z.object({ locations: z.array(z.string()) }),
	totalCostUsd: z.number(),
	qualifiedCount: z.number().int(),
	/** The newest real job, never a dry run; null when the campaign never ran. */
	latestRun: BackendRun.nullable(),
	/** The pipeline counts; only the single-campaign endpoints send it, and null until the campaign ran. */
	funnel: z
		.object({
			scraped: z.number().int(),
			scrapeCostUsd: z.number(),
			kept: z.number().int(),
			excluded: z.number().int(),
			excludedBy: z.array(z.object({ reason: z.string(), leads: z.number().int() })),
			cutShare: z.number(),
			qualified: z.number().int(),
			enriched: z.number().int(),
			enrichCostUsd: z.number(),
			enriching: z.boolean(),
		})
		.nullable(),
});
export type BackendCampaign = z.infer<typeof BackendCampaign>;
export const BackendCampaignList = z.array(BackendCampaign);

/**
 * What a write endpoint answers (docs/api.md Writes): the saved thing plus
 * the CLI's non-blocking warnings.
 */
export const BackendSaveResult = <S extends z.ZodType>(saved: S) => z.object({ saved, warnings: z.array(z.string()) });

/**
 * GET /api/company in full: the UI shape plus the fields only the CLI edits.
 * The save merges the UI profile over this, so a PUT keeps them.
 */
export const BackendCompanyProfile = z.object({
	name: z.string(),
	intro: z.string().nullable(),
	services: z.array(z.object({ name: z.string(), price: z.string().nullable(), deliveryTime: z.string().nullable() })),
	entryOffer: z.string().nullable(),
	area: z.array(z.string()),
	clients: z.array(z.object({ name: z.string(), phone: z.string().nullable() })),
	cases: z.array(
		z.object({
			sector: z.string().nullable(),
			client: z.string().nullable(),
			problem: z.string().nullable(),
			built: z.string().nullable(),
			result: z.string().nullable(),
			mayName: z.boolean(),
		}),
	),
	objections: z.array(z.object({ objection: z.string(), answer: z.string() })),
	weeklyCapacity: z.number().int(),
	quarterTarget: z.object({ newClients: z.number().int().nullable(), revenueKz: z.number().nullable() }).nullable(),
});
export type BackendCompanyProfile = z.infer<typeof BackendCompanyProfile>;

/** GET /api/usage?month=YYYY-MM. */
export const BackendUsage = z.object({
	apify: z.object({ costUsd: z.number() }),
	llm: z.object({ costUsd: z.number() }),
	/** A null slug holds the spend of deleted campaigns and of LLM calls made outside one. */
	byCampaign: z.array(z.object({ slug: z.string().nullable(), apifyUsd: z.number(), llmUsd: z.number() })),
	budgetUsd: z.number(),
	/** This UTC month, whatever the filter: spend plus what running jobs and unpriced runs reserve. */
	committedUsd: z.number(),
});
export type BackendUsage = z.infer<typeof BackendUsage>;

/** GET /api/usage/entries?month=YYYY-MM: one Apify run or LLM call. */
export const BackendUsageEntry = z.object({
	kind: z.enum(["apify", "llm"]),
	at: z.iso.datetime({ offset: true }),
	campaignSlug: z.string().nullable(),
	label: z.string(),
	costUsd: z.number().nullable(),
});
export type BackendUsageEntry = z.infer<typeof BackendUsageEntry>;
export const BackendUsageEntries = z.array(BackendUsageEntry);

/** One row of GET /api/campaigns/{slug}/leads and GET /api/leads/{id}. */
export const BackendLead = z.object({
	id: z.number().int(),
	campaignSlug: z.string(),
	stage: z.enum(["QUALIFIED", "BELOW_CUT", "EXCLUDED"]),
	status: z.enum(["NEW", "CONTACTED", "NO_ANSWER", "INTERESTED", "MEETING", "PROPOSAL_SENT", "WON", "LOST"]),
	/** Set when the lead was marked LOST: the five reasons from ADR 0012 and 0020. */
	lostReason: z.enum(["NO_BUDGET", "WRONG_PERSON", "HAS_SUPPLIER", "NOT_INTERESTED", "NOT_NOW"]).nullable(),
	/** Free-text note stored with the outcome. */
	note: z.string().nullable(),
	score: z.number().int(),
	breakdown: z.array(z.object({ code: z.string(), points: z.number().int(), reason: z.string() })),
	stageReason: z.string().nullable(),
	name: z.string(),
	category: z.string().nullable(),
	address: z.string().nullable(),
	neighborhood: z.string().nullable(),
	phoneE164: z.string().nullable(),
	phoneMobile: z.boolean(),
	website: z.string().nullable(),
	websiteKind: z.enum(["NONE", "SOCIAL_ONLY", "OWN"]),
	rating: z.number().nullable(),
	reviewsCount: z.number().int(),
	mapsUrl: z.string().nullable(),
	complaintKinds: z.array(z.string()),
	/** Null until enrichment writes one, or `POST /api/leads/{id}/pitch` does (ADR 0040). */
	pitch: z.string().nullable(),
	whatsappLink: z.string().nullable(),
});
export type BackendLead = z.infer<typeof BackendLead>;
export const BackendLeadList = z.array(BackendLead);
