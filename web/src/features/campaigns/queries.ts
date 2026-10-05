import { queryOptions, useMutation, useQueryClient } from "@tanstack/react-query";
import { createCampaign, fetchCampaign, fetchCampaigns } from "./api";
import type { CampaignCreateInput } from "./schema";

// Every key starts with "campaigns", and a campaign's runs nest under its detail key.
export const campaignsQuery = () => queryOptions({ queryKey: ["campaigns", "list"], queryFn: () => fetchCampaigns() });

export const campaignQuery = (slug: string) =>
	queryOptions({ queryKey: ["campaigns", "detail", slug], queryFn: () => fetchCampaign({ data: slug }) });

/** Creates a campaign, then refreshes the list. Backend errors reach the UI verbatim. */
export const useCreateCampaign = () => {
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: (input: CampaignCreateInput) => createCampaign({ data: input }),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: ["campaigns"] }),
	});
};
