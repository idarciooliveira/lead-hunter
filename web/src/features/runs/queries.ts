import { queryOptions, useMutation, useQueryClient } from "@tanstack/react-query";
import { campaignQuery } from "#/features/campaigns/queries";
import { fetchCampaignRuns, previewEnrichment, previewScrape, startEnrichment, startScrape } from "./api";
import type { Run } from "./schema";

/** How often a page with a running job polls it (ADR 0033). */
const POLL_MS = 3_000;

export const campaignRunsQuery = (slug: string) =>
	queryOptions({
		queryKey: [...campaignQuery(slug).queryKey, "runs"],
		queryFn: () => fetchCampaignRuns(slug),
		refetchInterval: (query) => (query.state.data?.some((r: Run) => r.status === "RUNNING") ? POLL_MS : false),
	});

export const usePreviewScrape = () => useMutation({ mutationFn: (slug: string) => previewScrape(slug) });

export const usePreviewEnrichment = () => useMutation({ mutationFn: (slug: string) => previewEnrichment(slug) });

export type StartRun = { slug: string; kind: "SCRAPE"; allowOverLimit: boolean } | { slug: string; kind: "ENRICH" };

/** Starts a job, then refreshes the campaigns (state, history) and the usage it may spend. */
export const useStartRun = () => {
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: (start: StartRun) =>
			start.kind === "SCRAPE" ? startScrape(start.slug, start.allowOverLimit) : startEnrichment(start.slug),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: ["campaigns"] }),
	});
};
