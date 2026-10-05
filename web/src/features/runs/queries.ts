import { queryOptions } from "@tanstack/react-query";
import { fetchCampaignRuns } from "./api";

export const campaignRunsQuery = (slug: string) =>
	queryOptions({ queryKey: ["campaigns", slug, "runs"], queryFn: () => fetchCampaignRuns(slug) });
