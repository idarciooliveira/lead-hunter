import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { useState } from "react";
import { Page, PageHeader } from "#/components/page-header";
import { StatBar } from "#/components/stat-bar";
import { Button } from "#/components/ui/button";
import { Card, CardHeader } from "#/components/ui/card";
import { Chip } from "#/components/ui/chip";
import { Cost } from "#/components/ui/cost";
import { FunnelCards } from "#/features/campaigns/components/funnel";
import { type RunDialog, RunDialogs } from "#/features/campaigns/components/run-dialogs";
import { CAMPAIGN_STATE } from "#/features/campaigns/model";
import { campaignQuery } from "#/features/campaigns/queries";
import { RunTable } from "#/features/runs/components/run-table";
import { campaignRunsQuery } from "#/features/runs/queries";
import { NotFoundError } from "#/lib/fake-api";
import { estimate, percent, usd } from "#/lib/format";

export const Route = createFileRoute("/campanhas/$slug")({
	loader: async ({ context, params }) => {
		try {
			const [campaign] = await Promise.all([
				context.queryClient.ensureQueryData(campaignQuery(params.slug)),
				context.queryClient.ensureQueryData(campaignRunsQuery(params.slug)),
			]);
			return { name: campaign.name };
		} catch (e) {
			if (e instanceof NotFoundError) throw notFound();
			throw e;
		}
	},
	head: ({ loaderData }) => ({ meta: [{ title: `${loaderData?.name ?? "Campanha"} · Lead Hunter` }] }),
	component: CampaignPage,
});

function CampaignPage() {
	const { slug } = Route.useParams();
	const { data: campaign } = useSuspenseQuery(campaignQuery(slug));
	const { data: runs } = useSuspenseQuery(campaignRunsQuery(slug));
	const [dialog, setDialog] = useState<RunDialog>(null);
	const state = CAMPAIGN_STATE[campaign.state];
	const left = campaign.limitUsd - campaign.spendUsd;

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
						<Button onClick={() => setDialog("dry")}>
							Dry run <Cost>grátis</Cost>
						</Button>
						<Button variant="primary" onClick={() => setDialog("dry")}>
							Executar <Cost onAccent>{estimate(campaign.nextRun.costUsd)}</Cost>
						</Button>
						<Button asChild>
							<Link to="/leads" search={{ stage: "QUALIFIED" }}>
								Enriquecer <Cost>{estimate(campaign.enrichCostUsd)}</Cost>
							</Link>
						</Button>
					</>
				}
			/>
			<StatBar
				value={usd(campaign.spendUsd)}
				caption={`de ${usd(campaign.limitUsd)} de limite da campanha`}
				percent={percent(campaign.spendUsd, campaign.limitUsd)}
				label="Gasto da campanha"
				aside={<Chip tone={left < campaign.limitUsd / 2 ? "warn" : "ok"}>Restam {usd(left)}</Chip>}
			/>
			{campaign.funnel && <FunnelCards funnel={campaign.funnel} />}
			<Card className="overflow-hidden">
				<CardHeader
					title="Histórico de execuções"
					aside={<span className="text-xs text-mute">{runs.length} execuções</span>}
				/>
				<RunTable runs={runs} />
			</Card>
			<RunDialogs campaign={campaign} open={dialog} onOpenChange={setDialog} />
		</Page>
	);
}
