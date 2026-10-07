import { z } from "zod";

/** Who is signed in. Null in the fixture build, which has no login (ADR 0042). */
export const Viewer = z.object({
	name: z.string(),
	email: z.string(),
	orgName: z.string(),
});
export type Viewer = z.infer<typeof Viewer>;
