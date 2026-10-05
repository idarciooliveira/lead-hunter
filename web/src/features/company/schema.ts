import { z } from "zod";

/** The company profile campaigns propose services from (ADR 0019, 0029). */
export const CompanyProfile = z.object({
	name: z.string(),
	services: z.array(
		z.object({ name: z.string(), description: z.string(), priceRange: z.string(), timeline: z.string() }),
	),
	target: z.object({ sectors: z.array(z.string()), size: z.string(), zones: z.array(z.string()), decider: z.string() }),
	pastClients: z.array(z.string()),
	cases: z.array(z.object({ client: z.string(), summary: z.string() })),
});
export type CompanyProfile = z.infer<typeof CompanyProfile>;

/** The profile needs at least this many cases before it counts as complete. */
export const MIN_CASES = 2;
