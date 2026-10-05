import { Chip } from "#/components/ui/chip";
import { DataTable, listColumns } from "#/components/ui/data-table";
import { usd, when } from "#/lib/format";
import type { UsageMonth } from "../schema";

type Event = UsageMonth["events"][number];
const col = listColumns<Event>();

const columns = col.columns([
	col.accessor("at", { header: "Quando", cell: (c) => <span className="text-mute">{when(c.getValue())}</span> }),
	col.accessor("campaign", {
		header: "Campanha",
		cell: (c) => c.getValue() ?? <span className="text-mute">sem campanha</span>,
	}),
	col.accessor("source", {
		header: "Fonte",
		cell: (c) => (
			<Chip tone={c.getValue() === "APIFY" ? "acc" : "info"}>{c.getValue() === "APIFY" ? "Apify" : "LLM"}</Chip>
		),
	}),
	col.accessor("detail", { header: "Detalhe", cell: (c) => <span className="text-mute">{c.getValue()}</span> }),
	col.accessor("costUsd", {
		header: "Custo",
		cell: (c) => {
			const cost = c.getValue();
			return cost === null ? (
				<span className="text-mute">sem preço</span>
			) : (
				<span className="font-mono">{usd(cost)}</span>
			);
		},
	}),
]);

export function UsageEvents({ events }: { events: Event[] }) {
	return (
		<DataTable
			label="Últimas execuções e chamadas"
			columns={columns}
			data={events}
			getRowId={(e) => `${e.at}-${e.source}-${e.detail}`}
			template="130px minmax(160px,1.4fr) 130px minmax(180px,2fr) 80px"
			minWidth={640}
			rowClassName="min-h-11"
			empty={<div className="px-4 py-6 text-mute">Nenhuma execução ou chamada neste mês.</div>}
		/>
	);
}
