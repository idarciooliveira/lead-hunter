import { queryOptions } from "@tanstack/react-query";
import { campaignQuery } from "#/features/campaigns/queries";
import { fetchCampaignRuns } from "./api";

export const campaignRunsQuery = (slug: string) =>
	queryOptions({ queryKey: [...campaignQuery(slug).queryKey, "runs"], queryFn: () => fetchCampaignRuns(slug) });
