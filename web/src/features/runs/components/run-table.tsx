import { Chip } from "#/components/ui/chip";
import { DataTable, listColumns } from "#/components/ui/data-table";
import { usd, when } from "#/lib/format";
import { RUN_KIND_LABEL, RUN_STATUS, runPlaces } from "../model";
import type { Run } from "../schema";

const col = listColumns<Run>();

const columns = col.columns([
	col.accessor("id", { header: "ID", cell: (c) => <span className="font-mono">{c.getValue()}</span> }),
	col.accessor("startedAt", { header: "Quando", cell: (c) => <span className="text-mute">{when(c.getValue())}</span> }),
	col.accessor("kind", { header: "Tipo", cell: (c) => RUN_KIND_LABEL[c.getValue()] }),
	col.accessor("status", {
		header: "Estado",
		cell: (c) => <Chip tone={RUN_STATUS[c.getValue()].tone}>{RUN_STATUS[c.getValue()].label}</Chip>,
	}),
	col.display({
		id: "places",
		header: "Lugares",
		cell: ({ row }) => <span className="font-mono">{runPlaces(row.original)}</span>,
	}),
	col.accessor("costUsd", { header: "Custo", cell: (c) => <span className="font-mono">{usd(c.getValue())}</span> }),
]);

export function RunTable({ runs }: { runs: Run[] }) {
	return (
		<DataTable
			label="Histórico de execuções"
			columns={columns}
			data={runs}
			getRowId={(r) => r.id}
			template="100px 110px 120px 90px 90px 90px"
			minWidth={780}
			rowClassName="min-h-11"
			empty={<div className="px-4 py-6 text-mute">Esta campanha ainda não correu.</div>}
		/>
	);
}
