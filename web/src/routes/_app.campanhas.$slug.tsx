import { useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { Page, PageHeader } from "#/components/page-header";
import { Button } from "#/components/ui/button";
import { Card, CardHeader } from "#/components/ui/card";
import { Chip } from "#/components/ui/chip";
import { Cost } from "#/components/ui/cost";
import { FunnelCards } from "#/features/campaigns/components/funnel";
import { type RunDialog, RunDialogs } from "#/features/campaigns/components/run-dialogs";
import { CAMPAIGN_STATE } from "#/features/campaigns/model";
import { campaignQuery } from "#/features/campaigns/queries";
import { RunTable } from "#/features/runs/components/run-table";
import { campaignRunsQuery, usePreviewEnrichment, usePreviewScrape, useStartRun } from "#/features/runs/queries";
import { usd } from "#/lib/format";

export const Route = createFileRoute("/_app/campanhas/$slug")({
	// An unknown slug throws notFound() from the server function, so the router shows its not-found page.
	loader: async ({ context, params }) => {
		const [campaign] = await Promise.all([
			context.queryClient.ensureQueryData(campaignQuery(params.slug)),
			context.queryClient.ensureQueryData(campaignRunsQuery(params.slug)),
		]);
		return { name: campaign.name };
	},
	head: ({ loaderData }) => ({ meta: [{ title: `${loaderData?.name ?? "Campanha"} · Lead Hunter` }] }),
	component: CampaignPage,
});

function CampaignPage() {
	const { slug } = Route.useParams();
	const queryClient = useQueryClient();
	const { data: campaign } = useSuspenseQuery(campaignQuery(slug));
	const { data: runs } = useSuspenseQuery(campaignRunsQuery(slug));
	const [dialog, setDialog] = useState<RunDialog>(null);
	const previewScrape = usePreviewScrape();
	const previewEnrichment = usePreviewEnrichment();
	const start = useStartRun();
	const state = CAMPAIGN_STATE[campaign.state];
	const running = runs.some((r) => r.status === "RUNNING");
	const previewError = previewScrape.error ?? previewEnrichment.error;

	// A job that just finished changed the campaign, its leads and the month's spend.
	const wasRunning = useRef(running);
	useEffect(() => {
		if (wasRunning.current && !running) {
			for (const queryKey of [["campaigns"], ["leads"], ["usage"]]) queryClient.invalidateQueries({ queryKey });
		}
		wasRunning.current = running;
	}, [running, queryClient]);

	// Each click is a new dry run, so only its own error stays on screen.
	const openScrape = () => {
		previewEnrichment.reset();
		previewScrape.mutate(slug, { onSuccess: (plan) => setDialog({ kind: "SCRAPE", plan }) });
	};
	const openEnrich = () => {
		previewScrape.reset();
		previewEnrichment.mutate(slug, { onSuccess: (plan) => setDialog({ kind: "ENRICH", plan }) });
	};
	const onOpenChange = (open: RunDialog) => {
		if (open === null) start.reset();
		setDialog(open);
	};
	const onStart = (allowOverLimit: boolean) => {
		if (!dialog) return;
		const run =
			dialog.kind === "SCRAPE" ? { slug, kind: "SCRAPE" as const, allowOverLimit } : { slug, kind: "ENRICH" as const };
		start.mutate(run, { onSuccess: () => onOpenChange(null) });
	};

	return (
		<Page>
			<PageHeader
				before={
					<Button size="sm" asChild>
						<Link to="/campanhas">Campanhas</Link>
					</Button>
				}
				title={
					<div className="flex flex-wrap items-center gap-3">
						<h1 className="m-0 text-xl font-semibold tracking-[-0.01em]">{campaign.name}</h1>
						<Chip tone={state.tone}>{state.label}</Chip>
						<span className="font-mono text-mute">{campaign.slug}</span>
					</div>
				}
				subtitle={`${campaign.sector} · ${campaign.service} · ${campaign.locations.join(", ")}`}
				actions={
					<>
						<Button variant="primary" disabled={running || previewScrape.isPending} onClick={openScrape}>
							Executar <Cost onAccent>dry run grátis</Cost>
						</Button>
						<Button disabled={running || previewEnrichment.isPending} onClick={openEnrich}>
							Enriquecer <Cost>dry run grátis</Cost>
						</Button>
					</>
				}
			/>
			{previewError && (
				<div role="alert" className="text-sm text-bad">
					{previewError.message}
				</div>
			)}
			<Card className="flex flex-wrap gap-8 p-4">
				<div>
					<span className="font-mono text-xl font-semibold">{usd(campaign.spendUsd)}</span>{" "}
					<span className="text-mute">gastos em Apify e LLM</span>
				</div>
				<div>
					<span className="font-mono text-xl font-semibold">{campaign.qualifiedCount}</span>{" "}
					<span className="text-mute">leads qualificados</span>
				</div>
			</Card>
			{campaign.funnel && <FunnelCards funnel={campaign.funnel} />}
			<Card className="overflow-hidden">
				<CardHeader
					title="Histórico de execuções"
					aside={<span className="text-xs text-mute">{runs.length} execuções</span>}
				/>
				<RunTable runs={runs} />
			</Card>
			<RunDialogs
				open={dialog}
				onOpenChange={onOpenChange}
				onStart={onStart}
				pending={start.isPending}
				error={start.error?.message ?? null}
			/>
		</Page>
	);
}
