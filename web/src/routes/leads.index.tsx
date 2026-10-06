import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link, stripSearchParams } from "@tanstack/react-router";
import { useCallback, useState } from "react";
import { z } from "zod";
import { ExportExcelButton } from "#/components/export-excel-button";
import { Page, PageHeader } from "#/components/page-header";
import { FoxState } from "#/components/states";
import { Button } from "#/components/ui/button";
import { Card } from "#/components/ui/card";
import { Chip, PlannedChip } from "#/components/ui/chip";
import { Segmented } from "#/components/ui/segmented";
import { LeadCards } from "#/features/leads/components/lead-cards";
import { LeadTable } from "#/features/leads/components/lead-table";
import { countByStage, filterLeads, STAGE_FILTERS } from "#/features/leads/model";
import { leadsQuery } from "#/features/leads/queries";
import { LeadStage } from "#/features/leads/schema";
import { useHotkeys } from "#/lib/use-hotkeys";
import { cn } from "#/lib/utils";

const DEFAULTS = { stage: "ALL", minScore: 0, site: false, phone: false } as const;

const search = z.object({
	stage: z
		.enum(["ALL", ...LeadStage.options])
		.catch(DEFAULTS.stage)
		.default(DEFAULTS.stage),
	minScore: z.coerce.number().int().min(0).max(100).catch(DEFAULTS.minScore).default(DEFAULTS.minScore),
	site: z.boolean().catch(DEFAULTS.site).default(DEFAULTS.site),
	phone: z.boolean().catch(DEFAULTS.phone).default(DEFAULTS.phone),
});

export const Route = createFileRoute("/leads/")({
	validateSearch: search,
	// Keep the URL short: only filters that differ from the defaults are written.
	search: { middlewares: [stripSearchParams(DEFAULTS)] },
	loader: ({ context }) => context.queryClient.ensureQueryData(leadsQuery()),
	head: () => ({ meta: [{ title: "Leads · Lead Hunter" }] }),
	component: LeadsPage,
});

function LeadsPage() {
	const { data: all } = useSuspenseQuery(leadsQuery());
	const filters = Route.useSearch();
	const navigate = Route.useNavigate();
	const [selected, setSelected] = useState<Record<string, boolean>>({});
	const [exported, setExported] = useState(false);

	const leads = filterLeads(all, {
		stage: filters.stage,
		minScore: filters.minScore,
		hasSite: filters.site,
		hasPhone: filters.phone,
	});
	const counts = countByStage(all);
	const selectedCount = Object.values(selected).filter(Boolean).length;
	const setFilter = (patch: Partial<z.infer<typeof search>>) =>
		navigate({ search: (prev) => ({ ...prev, ...patch }), replace: true });
	useHotkeys({ e: () => setExported(true) });
	const toggle = useCallback((id: string) => setSelected((s) => ({ ...s, [id]: !s[id] })), []);

	const empty = (
		<FoxState
			title="Nenhum lead com estes filtros"
			text="Baixa a pontuação mínima ou limpa os filtros."
			action={
				<Button asChild>
					<Link to="/leads" search={{}}>
						Limpar filtros
					</Link>
				</Button>
			}
		/>
	);

	return (
		<Page>
			<PageHeader
				title="Leads"
				subtitle={`${leads.length === all.length ? leads.length : `${leads.length} de ${all.length}`} leads em todas as campanhas, ordenados por pontuação`}
				actions={<ExportExcelButton onClick={() => setExported(true)} shortcut="E" />}
			/>
			{exported && (
				<Chip tone="ok" role="status">
					Ficheiro leads.xlsx exportado com {leads.length} linhas
				</Chip>
			)}

			<div className="-mx-4 flex gap-2 overflow-x-auto px-4 md:hidden">
				{STAGE_FILTERS.map((f) => (
					<button
						type="button"
						key={f.value}
						aria-pressed={filters.stage === f.value}
						onClick={() => setFilter({ stage: f.value })}
						className={cn(
							"inline-flex h-11 flex-none cursor-pointer items-center gap-1.5 whitespace-nowrap rounded-full border border-line2 bg-panel px-3.5 font-medium",
							filters.stage === f.value && "border-acc bg-acc-soft text-acc-tx",
						)}
					>
						{f.label} <span className="font-mono text-[11px]">{counts[f.value]}</span>
					</button>
				))}
			</div>

			<Card className="hidden flex-wrap items-center gap-5 px-4 py-3 md:flex">
				<Segmented
					label="Estágio"
					value={filters.stage}
					onChange={(stage) => setFilter({ stage })}
					options={STAGE_FILTERS.map((f) => ({ value: f.value, label: f.label, count: counts[f.value] }))}
				/>
				<label className="flex items-center gap-2">
					<span className="text-mute">Pontuação mínima</span>
					<input
						type="range"
						min={0}
						max={100}
						value={filters.minScore}
						onChange={(e) => setFilter({ minScore: Number(e.target.value) })}
						className="w-[140px]"
					/>
					<span className="w-6 font-mono">{filters.minScore}</span>
				</label>
				<label className="flex items-center gap-1.5">
					<input type="checkbox" checked={filters.site} onChange={() => setFilter({ site: !filters.site })} />
					Tem website
				</label>
				<label className="flex items-center gap-1.5">
					<input type="checkbox" checked={filters.phone} onChange={() => setFilter({ phone: !filters.phone })} />
					Tem telefone
				</label>
			</Card>

			<Card className="hidden overflow-hidden md:block">
				<LeadTable leads={leads} selected={selected} onToggle={toggle} empty={empty} />
			</Card>
			<div className="md:hidden">{leads.length ? <LeadCards leads={leads} /> : empty}</div>

			{selectedCount > 0 && (
				<Card className="sticky bottom-4 flex flex-wrap items-center gap-3 border-acc px-4 py-2.5 shadow-[0_4px_18px_rgba(0,0,0,.14)]">
					<span className="font-mono font-semibold">{selectedCount} seleccionados</span>
					<Button size="sm" onClick={() => setSelected({})}>
						Marcar como contactado
					</Button>
					<Button size="sm" asChild>
						<Link to="/hoje">
							Enviar para Hoje <PlannedChip />
						</Link>
					</Button>
					<ExportExcelButton onClick={() => setExported(true)} size="sm" />
					<div className="flex-1" />
					<Button size="sm" onClick={() => setSelected({})}>
						Limpar
					</Button>
				</Card>
			)}
		</Page>
	);
}
