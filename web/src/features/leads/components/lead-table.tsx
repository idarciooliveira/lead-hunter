import { Link } from "@tanstack/react-router";
import { useMemo } from "react";
import { WhatsAppLink } from "#/components/contact-actions";
import { DataTable, listColumns } from "#/components/ui/data-table";
import { rating } from "#/lib/format";
import type { Lead } from "../schema";
import { ScoreCell } from "./score";
import { ContactChip, StageChip } from "./status-chips";

const col = listColumns<Lead>();

export function LeadTable({
	leads,
	selected,
	onToggle,
	empty,
}: {
	leads: Lead[];
	selected: Record<string, boolean>;
	onToggle: (id: string) => void;
	empty: React.ReactNode;
}) {
	const columns = useMemo(
		() =>
			col.columns([
				col.display({
					id: "select",
					header: () => null,
					cell: ({ row }) => (
						<input
							type="checkbox"
							checked={!!selected[row.original.id]}
							onChange={() => onToggle(row.original.id)}
							aria-label={`Seleccionar ${row.original.name}`}
							className="size-4"
						/>
					),
				}),
				col.accessor("rank", {
					header: "#",
					cell: (c) => <span className="font-mono text-mute">{c.getValue() ?? "–"}</span>,
				}),
				col.accessor("name", {
					header: "Negócio",
					cell: ({ row }) => (
						<div className="min-w-0">
							<Link to="/leads/$leadId" params={{ leadId: row.original.id }} className="font-semibold">
								{row.original.name}
							</Link>
							<div className="font-mono text-[11px] text-mute">{row.original.phone ?? "Sem telefone"}</div>
						</div>
					),
				}),
				col.accessor("category", { header: "Categoria" }),
				col.accessor("area", { header: "Área", cell: (c) => <span className="text-mute">{c.getValue()}</span> }),
				col.accessor("rating", {
					header: "Avaliação",
					cell: ({ row }) => (
						<span className="font-mono">
							{rating(row.original.rating)} <span className="text-mute">({row.original.reviewCount})</span>
						</span>
					),
				}),
				col.accessor("score", { header: "Pontuação", cell: ({ row }) => <ScoreCell lead={row.original} /> }),
				col.accessor("stage", { header: "Estágio", cell: (c) => <StageChip stage={c.getValue()} /> }),
				col.accessor("status", { header: "Contacto", cell: ({ row }) => <ContactChip lead={row.original} /> }),
				col.display({
					id: "whatsapp",
					header: () => null,
					cell: ({ row }) => (
						<WhatsAppLink phone={row.original.phone} message={row.original.pitch} iconOnly size="icon-sm" />
					),
				}),
			]),
		[selected, onToggle],
	);
	return (
		<DataTable
			label="Leads"
			columns={columns}
			data={leads}
			getRowId={(l) => l.id}
			template="28px 40px minmax(190px,2fr) 130px 110px 110px 140px 130px 120px 40px"
			minWidth={1160}
			rowClassName="min-h-12 hover:bg-soft"
			empty={empty}
		/>
	);
}
