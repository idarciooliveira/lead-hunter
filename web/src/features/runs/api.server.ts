import { CAMPAIGNS } from "#/features/campaigns/fixtures";
import { apiBaseUrl } from "#/lib/api-config.server";
import { type BackendRun, BackendRunList, BackendRun as BackendRunSchema } from "#/lib/api-contract";
import { fakeResponse, NotFoundError } from "#/lib/fake-api";
import { apiFetch, apiMutate } from "#/lib/http.server";
import { FIXTURE_ENRICH_BATCH, fixtureScrapePlan, RUNS } from "./fixtures";
import { EnrichPlan, Run, RunList, ScrapePlan } from "./schema";

/** GET /api/campaigns/{slug}/runs, newest first. */
export async function fetchCampaignRuns(slug: string): Promise<Run[]> {
	if (apiBaseUrl() === null) {
		const runs = RUNS.filter((r) => r.campaignSlug === slug).sort((a, b) => b.startedAt.localeCompare(a.startedAt));
		return fakeResponse(RunList, runs);
	}
	const runs = await apiFetch(BackendRunList, `/campaigns/${encodeURIComponent(slug)}/runs`);
	return fakeResponse(RunList, runs.map(toRun));
}

/** POST /api/campaigns/{slug}/runs?dryRun=true: the free estimate. It is recorded as a dry run. */
export async function previewScrape(slug: string): Promise<ScrapePlan> {
	if (apiBaseUrl() === null) {
		const campaign = fixtureCampaign(slug);
		return fakeResponse(ScrapePlan, fixtureScrapePlan(campaign.locations, campaign.sector));
	}
	return apiMutate(ScrapePlan, `/campaigns/${encodeURIComponent(slug)}/runs?dryRun=true`, "POST");
}

/** POST /api/campaigns/{slug}/runs: starts a scrape job (202). `allowOverLimit` is the explicit opt-in (ADR 0033). */
export async function startScrape(slug: string, allowOverLimit: boolean): Promise<Run> {
	if (apiBaseUrl() === null) return fakeStart(slug, "SCRAPE");
	const run = await apiMutate(
		BackendRunSchema,
		`/campaigns/${encodeURIComponent(slug)}/runs?allowOverLimit=${allowOverLimit}`,
		"POST",
	);
	return fakeResponse(Run, toRun(run));
}

/** POST /api/campaigns/{slug}/enrichment?dryRun=true: leads waiting and the batch, with the CLI defaults. */
export async function previewEnrichment(slug: string): Promise<EnrichPlan> {
	if (apiBaseUrl() === null) {
		const campaign = fixtureCampaign(slug);
		return fakeResponse(EnrichPlan, { pending: campaign.qualifiedCount, batch: FIXTURE_ENRICH_BATCH, maxReviews: 20 });
	}
	return apiMutate(EnrichPlan, `/campaigns/${encodeURIComponent(slug)}/enrichment?dryRun=true`, "POST");
}

/** POST /api/campaigns/{slug}/enrichment: starts an enrichment job (202). */
export async function startEnrichment(slug: string): Promise<Run> {
	if (apiBaseUrl() === null) return fakeStart(slug, "ENRICH");
	const run = await apiMutate(BackendRunSchema, `/campaigns/${encodeURIComponent(slug)}/enrichment`, "POST");
	return fakeResponse(Run, toRun(run));
}

/** A backend job becomes a UI run. A dry run's `done` is its estimate. */
export function toRun(b: BackendRun): Run {
	return { ...b, id: String(b.id), estimated: b.kind === "DRY_RUN" };
}

function fixtureCampaign(slug: string) {
	const campaign = CAMPAIGNS.find((c) => c.slug === slug);
	if (!campaign) throw new NotFoundError(`campaign ${slug} not found`);
	return campaign;
}

/** Without an API a start lands in the in-memory history as finished, so the smoke build never polls. */
function fakeStart(slug: string, kind: "SCRAPE" | "ENRICH"): Promise<Run> {
	fixtureCampaign(slug);
	const run: Run = {
		id: `run_${RUNS.length + 1000}`,
		campaignSlug: slug,
		kind,
		status: "DONE",
		startedAt: new Date().toISOString(),
		done: 0,
		total: null,
		estimated: false,
		costUsd: 0,
		error: null,
	};
	RUNS.push(run);
	return fakeResponse(Run, run);
}
