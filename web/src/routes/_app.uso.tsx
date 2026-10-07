import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { z } from "zod";
import { Page, PageHeader } from "#/components/page-header";
import { Button } from "#/components/ui/button";
import { Card, CardHeader } from "#/components/ui/card";
import { Chip } from "#/components/ui/chip";
import { ProgressBar } from "#/components/ui/progress";
import { UsageEvents } from "#/features/usage/components/usage-events";
import { budgetState, shiftMonth, totalOf, usedOf } from "#/features/usage/model";
import { usageQuery } from "#/features/usage/queries";
import { monthLabel, percent, usd } from "#/lib/format";

export const Route = createFileRoute("/_app/uso")({
	validateSearch: z.object({
		month: z
			.string()
			.regex(/^\d{4}-\d{2}$/)
			.optional()
			.catch(undefined),
	}),
	loaderDeps: ({ search }) => ({ month: search.month }),
	loader: ({ context, deps }) =>
		Promise.all([
			context.queryClient.ensureQueryData(usageQuery()),
			context.queryClient.ensureQueryData(usageQuery(deps.month)),
		]),
	head: () => ({ meta: [{ title: "Uso e custos · Lead Hunter" }] }),
	component: UsagePage,
});

function UsagePage() {
	const { month: wanted } = Route.useSearch();
	const { data: current } = useSuspenseQuery(usageQuery());
	const { data: m } = useSuspenseQuery(usageQuery(wanted));
	const latest = current.month;
	const month = m.month;
	const navigate = Route.useNavigate();
	const total = totalOf(m);
	const used = usedOf(m);
	const state = budgetState(m);
	const max = Math.max(...m.byCampaign.map((c) => c.usd));
	const go = (delta: number) => navigate({ search: { month: shiftMonth(month, delta) }, replace: true });

	return (
		<Page>
			<PageHeader
				title="Uso e custos"
				subtitle={`Orçamento de $${m.budgetUsd} por mês. Apify cobra por lugar, o LLM por chamada.`}
				actions={
					<div className="flex items-center gap-2">
						<Button size="icon" aria-label="Mês anterior" onClick={() => go(-1)}>
							<ChevronLeft className="size-4" aria-hidden />
						</Button>
						<span className="min-w-[120px] text-center font-medium">{monthLabel(m.month)}</span>
						<Button size="icon" aria-label="Mês seguinte" disabled={month >= latest} onClick={() => go(1)}>
							<ChevronRight className="size-4" aria-hidden />
						</Button>
					</div>
				}
			/>
			<Card className="flex flex-col gap-3 p-4">
				<div className="flex flex-wrap items-baseline justify-between gap-2">
					<div>
						<span className="font-mono text-[28px] font-semibold">{usd(total)}</span>{" "}
						<span className="text-mute">de {usd(m.budgetUsd)}</span>
					</div>
					<Chip tone={state.tone}>{state.label}</Chip>
				</div>
				<ProgressBar value={percent(used, m.budgetUsd)} label="Orçamento gasto" />
				{used > total && (
					<div className="text-mute">
						{usd(used)} gasto ou reservado por jobs em curso. Uma nova execução é recusada se passar o orçamento.
					</div>
				)}
				<div className="flex flex-wrap gap-6">
					<span>
						<span className="text-mute">Apify</span> <span className="font-mono font-semibold">{usd(m.apifyUsd)}</span>
					</span>
					<span>
						<span className="text-mute">LLM</span> <span className="font-mono font-semibold">{usd(m.llmUsd)}</span>
					</span>
				</div>
			</Card>
			<Card className="flex flex-col gap-3 p-4">
				<h2 className="m-0 text-base font-semibold">Gasto por campanha</h2>
				{m.byCampaign.length === 0 && <div className="text-mute">Sem gastos neste mês.</div>}
				{m.byCampaign.map((c) => (
					<div key={c.campaign} className="grid grid-cols-[minmax(110px,150px)_1fr_56px] items-center gap-3">
						<span>{c.campaign}</span>
						<ProgressBar value={percent(c.usd, max)} label={c.campaign} />
						<span className="text-right font-mono">{usd(c.usd)}</span>
					</div>
				))}
			</Card>
			<Card className="min-w-0 overflow-hidden">
				<CardHeader title="Últimas execuções e chamadas" />
				<UsageEvents events={m.events} />
			</Card>
		</Page>
	);
}
