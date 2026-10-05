import { apiBaseUrl } from "#/lib/api-config.server";
import {
	type BackendCampaign,
	BackendCampaignList,
	BackendCampaign as BackendCampaignSchema,
} from "#/lib/api-contract";
import { fakeResponse, NotFoundError } from "#/lib/fake-api";
import { apiFetch } from "#/lib/http.server";
import { CAMPAIGNS } from "./fixtures";
import { Campaign, CampaignList, type CampaignState } from "./schema";

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
