import { Link, useNavigate } from "@tanstack/react-router";
import { Chip } from "#/components/ui/chip";
import { DataTable, listColumns } from "#/components/ui/data-table";
import { usd } from "#/lib/format";
import { CAMPAIGN_STATE } from "../model";
import type { Campaign } from "../schema";

const col = listColumns<Campaign>();

const columns = col.columns([
	col.accessor("name", {
		header: "Campanha",
		cell: ({ row }) => (
			<div>
				<Link to="/campanhas/$slug" params={{ slug: row.original.slug }} className="font-semibold">
					{row.original.name}
				</Link>
				<div className="font-mono text-[11px] text-mute">{row.original.slug}</div>
			</div>
		),
	}),
	col.accessor("sector", { header: "Sector" }),
	col.accessor("service", { header: "Serviço proposto" }),
	col.accessor("locations", {
		header: "Localizações",
		cell: (c) => <span className="text-mute">{c.getValue().join(", ")}</span>,
	}),
	col.accessor("state", {
		header: "Estado",
		cell: (c) => <Chip tone={CAMPAIGN_STATE[c.getValue()].tone}>{CAMPAIGN_STATE[c.getValue()].label}</Chip>,
	}),
	col.accessor("qualifiedCount", {
		header: "Qualificados",
		cell: (c) => <span className="font-mono font-semibold">{c.getValue()}</span>,
	}),
	col.accessor("spendUsd", { header: "Apify", cell: (c) => <span className="font-mono">{usd(c.getValue())}</span> }),
]);

export function CampaignTable({ campaigns }: { campaigns: Campaign[] }) {
	const navigate = useNavigate();
	return (
		<DataTable
			label="Campanhas"
			columns={columns}
			data={campaigns}
			getRowId={(c) => c.slug}
			template="minmax(220px,2fr) 130px minmax(150px,1.2fr) 140px 110px 100px 90px"
			minWidth={1040}
			rowClassName="min-h-14"
			onRowClick={(c) => navigate({ to: "/campanhas/$slug", params: { slug: c.slug } })}
		/>
	);
}
