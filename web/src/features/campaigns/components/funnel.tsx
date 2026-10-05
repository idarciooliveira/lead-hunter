import { Card } from "#/components/ui/card";
import { Chip } from "#/components/ui/chip";
import { ProgressBar } from "#/components/ui/progress";
import { percent, usd } from "#/lib/format";
import { cn } from "#/lib/utils";
import type { Funnel } from "../schema";

type StageCard = { name: string; running: boolean; count: string; note: string; progress?: number };

function stages(f: Funnel): StageCard[] {
	return [
		{
			name: "Scrape",
			running: false,
			count: String(f.scraped),
			note: `lugares no Google Maps · ${usd(f.scrapeCostUsd)}`,
		},
		{ name: "Filter", running: false, count: String(f.kept), note: f.filterNote },
		{ name: "Stage 1 score", running: false, count: String(f.kept), note: "pontuados só com dados do Maps" },
		{
			name: `Cut (${Math.round(f.cutShare * 100)}%)`,
			running: false,
			count: String(f.qualified),
			note: "acima do corte, vão para o Stage 2",
		},
		{
			name: "Enrich",
			running: f.enriching,
			count: f.enriching ? `${f.enriched}/${f.qualified}` : String(f.enriched),
			note: `website e reviews · ${usd(f.enrichCostUsd)}${f.enriching ? " até agora" : ""}`,
			progress: f.enriching ? percent(f.enriched, f.qualified) : undefined,
		},
	];
}

/** The pipeline from ADR 0006 as five cards, with the running stage highlighted. */
export function FunnelCards({ funnel }: { funnel: Funnel }) {
	return (
		<Card className="p-4">
			<div className="flex flex-wrap items-stretch gap-2">
				{stages(funnel).map((s) => (
					<div
						key={s.name}
						className={cn(
							"flex min-w-[150px] flex-[1_1_150px] flex-col gap-1.5 rounded-lg border border-line p-3",
							s.running ? "bg-info-soft" : "bg-bg",
						)}
					>
						<div className="flex items-center justify-between">
							<span className="font-semibold">{s.name}</span>
							<Chip tone={s.running ? "info" : "ok"}>{s.running ? "a correr" : "feito"}</Chip>
						</div>
						<div className="font-mono text-[22px] font-semibold">{s.count}</div>
						<div className="min-h-[34px] text-xs text-mute">{s.note}</div>
						{s.progress !== undefined && <ProgressBar value={s.progress} label={`${s.name} progresso`} />}
					</div>
				))}
			</div>
		</Card>
	);
}
