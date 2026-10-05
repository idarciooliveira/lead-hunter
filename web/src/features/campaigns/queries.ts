import { queryOptions } from "@tanstack/react-query";
import { fetchCampaign, fetchCampaigns } from "./api";

export const campaignsQuery = () => queryOptions({ queryKey: ["campaigns"], queryFn: fetchCampaigns });

export const campaignQuery = (slug: string) =>
	queryOptions({ queryKey: ["campaigns", slug], queryFn: () => fetchCampaign(slug) });
