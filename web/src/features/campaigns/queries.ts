import { queryOptions } from "@tanstack/react-query";
import { fetchCampaign, fetchCampaigns } from "./api";

// Every key starts with "campaigns", and a campaign's runs nest under its detail key.
export const campaignsQuery = () => queryOptions({ queryKey: ["campaigns", "list"], queryFn: () => fetchCampaigns() });

export const campaignQuery = (slug: string) =>
	queryOptions({ queryKey: ["campaigns", "detail", slug], queryFn: () => fetchCampaign({ data: slug }) });
