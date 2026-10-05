import { z } from "zod";

/**
 * The backend shapes `docs/api.md` serves. Only the fields the UI reads are
 * listed; Zod strips the rest. As the API grows (pitches, audits, funnels),
 * the mappers in each feature's `api.ts` pick the new fields up here.
 */

/** One row of GET /api/campaigns, as far as the leads pages need it. */
export const BackendCampaign = z.object({ slug: z.string(), name: z.string() });
export type BackendCampaign = z.infer<typeof BackendCampaign>;
export const BackendCampaignList = z.array(BackendCampaign);

/** One row of GET /api/campaigns/{slug}/leads and GET /api/leads/{id}. */
export const BackendLead = z.object({
	id: z.number().int(),
	campaignSlug: z.string(),
	stage: z.enum(["QUALIFIED", "BELOW_CUT", "EXCLUDED"]),
	status: z.enum(["NEW", "CONTACTED", "NO_ANSWER", "INTERESTED", "MEETING", "PROPOSAL_SENT", "WON", "LOST"]),
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
	whatsappLink: z.string().nullable(),
});
export type BackendLead = z.infer<typeof BackendLead>;
export const BackendLeadList = z.array(BackendLead);
