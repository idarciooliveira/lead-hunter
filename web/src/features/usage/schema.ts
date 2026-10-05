import { z } from "zod";

/** One month of spend, as the `usage` command reports it (ADR 0021). */
export const UsageMonth = z.object({
	month: z.string().regex(/^\d{4}-\d{2}$/),
	budgetUsd: z.number(),
	apifyUsd: z.number(),
	llmUsd: z.number(),
	byCampaign: z.array(z.object({ campaign: z.string(), usd: z.number() })),
	events: z.array(
		z.object({
			at: z.iso.datetime({ offset: true }),
			campaign: z.string(),
			source: z.enum(["APIFY", "LLM"]),
			detail: z.string(),
			costUsd: z.number(),
		}),
	),
});
export type UsageMonth = z.infer<typeof UsageMonth>;
export const UsageMonths = z.array(UsageMonth);
