import { apiBaseUrl } from "#/lib/api-config.server";
import {
	type BackendCampaign,
	BackendCampaignList,
	BackendCampaign as BackendCampaignSchema,
	BackendSaveResult,
} from "#/lib/api-contract";
import { fakeResponse, NotFoundError } from "#/lib/fake-api";
import { apiFetch, apiMutate } from "#/lib/http.server";
import { CAMPAIGNS } from "./fixtures";
import { Campaign, type CampaignCreateInput, CampaignList, type CampaignState } from "./schema";

/** GET /api/campaigns, oldest first. */
export async function fetchCampaigns(): Promise<Campaign[]> {
	if (apiBaseUrl() === null) return fakeResponse(CampaignList, CAMPAIGNS);
	const campaigns = await apiFetch(BackendCampaignList, "/campaigns");
	return fakeResponse(CampaignList, campaigns.map(toCampaign));
}

/** GET /api/campaigns/{slug}. */
export async function fetchCampaign(slug: string): Promise<Campaign> {
	if (apiBaseUrl() === null) {
		const campaign = CAMPAIGNS.find((c) => c.slug === slug);
		if (!campaign) throw new NotFoundError(`campaign ${slug} not found`);
		return fakeResponse(Campaign, campaign);
	}
	const campaign = await apiFetch(BackendCampaignSchema, `/campaigns/${encodeURIComponent(slug)}`);
	return fakeResponse(Campaign, toCampaign(campaign));
}

/** What a write answers: the saved campaign and the CLI's non-blocking warnings. */
export type SaveCampaign = { saved: Campaign; warnings: string[] };

/**
 * Creates a campaign: POST /api/campaigns (docs/api.md Writes). Without an
 * API configured the campaign lands on the in-memory fixtures for the
 * session, so the pages keep working in the smoke build. A taken slug is an
 * error with the backend's message, like every other rule failure.
 */
export async function createCampaign(input: CampaignCreateInput): Promise<SaveCampaign> {
	if (apiBaseUrl() === null) {
		if (CAMPAIGNS.some((c) => c.slug === input.slug)) {
			throw new Error(`campaign '${input.slug}' already exists. Update it instead`);
		}
		const saved: Campaign = {
			slug: input.slug,
			name: input.name,
			sector: input.answers.sector,
			service: input.answers.service,
			locations: [...input.search.locations],
			state: "DRAFT",
			qualifiedCount: 0,
			spendUsd: 0,
			funnel: null,
		};
		CAMPAIGNS.push(saved);
		return { saved: await fakeResponse(Campaign, saved), warnings: [] };
	}
	const res = await apiMutate(BackendSaveResult(BackendCampaignSchema), "/campaigns", "POST", input);
	return { saved: await fakeResponse(Campaign, toCampaign(res.saved)), warnings: res.warnings };
}

/** Backend rows become UI campaigns. The state is the newest real job's, as the API reports it. */
export function toCampaign(b: BackendCampaign): Campaign {
	return {
		slug: b.slug,
		name: b.name,
		sector: b.answers.sector ?? "",
		service: b.answers.service ?? "",
		locations: b.search.locations,
		state: stateOf(b.latestRun),
		qualifiedCount: b.qualifiedCount,
		spendUsd: b.totalCostUsd,
		funnel: null,
	};
}

function stateOf(run: BackendCampaign["latestRun"]): CampaignState {
	if (run === null) return "DRAFT";
	if (run.status === "RUNNING") return run.kind === "ENRICH" ? "ENRICHING" : "RUNNING";
	return run.status;
}
