import { fakeResponse, NotFoundError } from "#/lib/fake-api";
import { CAMPAIGNS } from "./fixtures";
import { Campaign, CampaignList } from "./schema";

/** Becomes GET /api/campaigns. */
export async function fetchCampaigns(): Promise<Campaign[]> {
	return fakeResponse(CampaignList, CAMPAIGNS);
}

/** Becomes GET /api/campaigns/{slug}. */
export async function fetchCampaign(slug: string): Promise<Campaign> {
	const campaign = CAMPAIGNS.find((c) => c.slug === slug);
	if (!campaign) throw new NotFoundError(`campaign ${slug} not found`);
	return fakeResponse(Campaign, campaign);
}
