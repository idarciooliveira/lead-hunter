import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Page, PageHeader } from "#/components/page-header";
import { FoxState } from "#/components/states";
import { Button } from "#/components/ui/button";
import { Card } from "#/components/ui/card";
import { Kbd } from "#/components/ui/kbd";
import { CampaignTable } from "#/features/campaigns/components/campaign-table";
import { campaignsQuery } from "#/features/campaigns/queries";
import { totalOf } from "#/features/usage/model";
import { usageQuery } from "#/features/usage/queries";
import { usd } from "#/lib/format";

export const Route = createFileRoute("/_app/campanhas/")({
	loader: ({ context }) => context.queryClient.ensureQueryData(campaignsQuery()),
	head: () => ({ meta: [{ title: "Campanhas · Lead Hunter" }] }),
	component: CampaignsPage,
});

function NewCampaignButton() {
	return (
		<Button variant="primary" asChild>
			<Link to="/campanhas/nova">
				Nova campanha <Kbd onAccent>N</Kbd>
			</Link>
		</Button>
	);
}

function CampaignsPage() {
	const { data: campaigns } = useSuspenseQuery(campaignsQuery());
	const { data: usage } = useSuspenseQuery(usageQuery());
	const active = campaigns.filter((c) => c.state !== "DRAFT").length;
	return (
		<Page>
			<PageHeader
				title="Campanhas"
				subtitle={`${active} campanhas activas · ${usd(totalOf(usage))} gastos este mês em Apify e LLM`}
				actions={<NewCampaignButton />}
			/>
			<Card className="overflow-hidden">
				{campaigns.length ? (
					<CampaignTable campaigns={campaigns} />
				) : (
					<FoxState
						title="Ainda não há campanhas"
						text="Cria a primeira para procurar negócios no Google Maps."
						action={<NewCampaignButton />}
					/>
				)}
			</Card>
		</Page>
	);
}
