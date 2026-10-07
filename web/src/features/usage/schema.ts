import { z } from "zod";

/** One month of spend, as `usage --month` reports it (ADR 0021), with its newest runs and calls. */
export const UsageMonth = z.object({
	month: z.string().regex(/^\d{4}-\d{2}$/),
	budgetUsd: z.number(),
	apifyUsd: z.number(),
	llmUsd: z.number(),
	/**
	 * What the budget check counts this month: spend plus what running jobs and unpriced runs reserve (ADR 0044).
	 * Null for any other month.
	 */
	committedUsd: z.number().nullable(),
	byCampaign: z.array(z.object({ campaign: z.string(), usd: z.number() })),
	events: z.array(
		z.object({
			at: z.iso.datetime({ offset: true }),
			/** Null for LLM calls made outside a campaign. */
			campaign: z.string().nullable(),
			source: z.enum(["APIFY", "LLM"]),
			detail: z.string(),
			/** Null when the provider reported no cost. */
			costUsd: z.number().nullable(),
		}),
	),
});
export type UsageMonth = z.infer<typeof UsageMonth>;
