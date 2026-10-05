import { fakeResponse } from "#/lib/fake-api";
import { RUNS } from "./fixtures";
import { type Run, RunList } from "./schema";

/** Newest first. Becomes GET /api/campaigns/{slug}/runs. */
export async function fetchCampaignRuns(slug: string): Promise<Run[]> {
	const runs = RUNS.filter((r) => r.campaignSlug === slug).sort((a, b) => b.startedAt.localeCompare(a.startedAt));
	return fakeResponse(RunList, runs);
}
