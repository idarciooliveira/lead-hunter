import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { ExportExcelButton } from "#/components/export-excel-button";
import { Page, PageHeader } from "#/components/page-header";
import { StatBar } from "#/components/stat-bar";
import { Chip } from "#/components/ui/chip";
import { Kbd } from "#/components/ui/kbd";
import { type QueueState, TodayQueue } from "#/features/leads/components/today-queue";
import { DAILY_GOAL, type Outcome } from "#/features/leads/model";
import { todayQueueQuery, useMarkLead } from "#/features/leads/queries";
import { longDay, percent } from "#/lib/format";
import { useHotkeys } from "#/lib/use-hotkeys";

export const Route = createFileRoute("/hoje")({
	loader: ({ context }) => context.queryClient.ensureQueryData(todayQueueQuery()),
	head: () => ({ meta: [{ title: "Hoje · Lead Hunter" }] }),
	component: TodayPage,
});

function TodayPage() {
	const { data: leads } = useSuspenseQuery(todayQueueQuery());
	const [state, setState] = useState<QueueState>({ done: {} });
	const [exported, setExported] = useState(false);
	const mark = useMarkLead();
	const done = leads.filter((l) => state.done[l.id]).length;
	const today = new Date();
	const fileDate = today.toISOString().slice(0, 10);

	useHotkeys({ e: () => setExported(true) });

	const onMark = (id: string, outcome: Outcome) =>
		mark.mutate({
			id,
			input: outcome === "NOT_NOW" ? { status: "LOST", lostReason: "NOT_NOW" } : { status: outcome, lostReason: null },
		});

	return (
		<Page>
			<PageHeader
				title="Hoje"
				subtitle={`${longDay(today)}. Os ${leads.length} leads com melhor pontuação ainda por contactar.`}
				actions={<ExportExcelButton onClick={() => setExported(true)} shortcut="E" />}
			/>
			{exported && (
				<Chip tone="ok" className="h-8 w-fit" role="status">
					Ficheiro leads-hoje-{fileDate}.xlsx exportado com {leads.length} linhas
				</Chip>
			)}
			<StatBar
				value={`${done}/${leads.length}`}
				caption="contactados"
				percent={percent(done, leads.length)}
				label="Contactados hoje"
				aside={<div className="text-mute">Meta diária: {DAILY_GOAL} contactos</div>}
			/>
			<TodayQueue
				leads={leads}
				state={state}
				// A single mutation at a time: while one mark is in flight every
				// button stays disabled, so a second click cannot overtake it.
				pendingId={mark.isPending ? (mark.variables?.id ?? "") : null}
				error={mark.isError ? mark.error.message : null}
				onToggle={(id) => setState((s) => ({ ...s, done: { ...s.done, [id]: !s.done[id] } }))}
				onMark={onMark}
			/>
			<div className="hidden flex-wrap gap-4 text-xs text-mute md:flex">
				<span>
					<Kbd>J</Kbd> <Kbd>K</Kbd> navegar
				</span>
				<span>
					<Kbd>W</Kbd> abrir WhatsApp
				</span>
				<span>
					<Kbd>C</Kbd> marcar contactado
				</span>
				<span>
					<Kbd>N</Kbd> não agora
				</span>
			</div>
		</Page>
	);
}
