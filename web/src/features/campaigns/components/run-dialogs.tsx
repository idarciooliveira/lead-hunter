import { type ReactNode, useState } from "react";
import { Button } from "#/components/ui/button";
import { Chip } from "#/components/ui/chip";
import { Cost } from "#/components/ui/cost";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogTitle } from "#/components/ui/dialog";
import type { EnrichPlan, ScrapePlan } from "#/features/runs/schema";
import { estimate } from "#/lib/format";

/** The free estimate a dialog confirms; null while closed. */
export type RunDialog = { kind: "SCRAPE"; plan: ScrapePlan } | { kind: "ENRICH"; plan: EnrichPlan } | null;

/**
 * The confirmation flow from ADR 0033: the free dry run first, then a start.
 * A scrape past the per-run place limit needs an explicit opt-in. Backend
 * errors (a job already running, the budget) show verbatim.
 */
export function RunDialogs({
	open,
	onOpenChange,
	onStart,
	pending,
	error,
}: {
	open: RunDialog;
	onOpenChange: (open: RunDialog) => void;
	onStart: (allowOverLimit: boolean) => void;
	pending: boolean;
	error: string | null;
}) {
	const [understood, setUnderstood] = useState(false);
	const close = () => {
		setUnderstood(false);
		onOpenChange(null);
	};
	const failure = error && (
		<div role="alert" className="text-sm text-bad">
			{error}
		</div>
	);

	return (
		<>
			<Dialog open={open?.kind === "SCRAPE"} onOpenChange={(o) => !o && close()}>
				<DialogContent className={open?.kind === "SCRAPE" && open.plan.overLimit ? "border-bad" : undefined}>
					{open?.kind === "SCRAPE" && (
						<ScrapeBody
							plan={open.plan}
							understood={understood}
							onUnderstood={setUnderstood}
							onStart={() => onStart(open.plan.overLimit)}
							pending={pending}
							failure={failure}
						/>
					)}
				</DialogContent>
			</Dialog>

			<Dialog open={open?.kind === "ENRICH"} onOpenChange={(o) => !o && close()}>
				<DialogContent>
					{open?.kind === "ENRICH" && (
						<EnrichBody plan={open.plan} onStart={() => onStart(false)} pending={pending} failure={failure} />
					)}
				</DialogContent>
			</Dialog>
		</>
	);
}

function ScrapeBody({
	plan,
	understood,
	onUnderstood,
	onStart,
	pending,
	failure,
}: {
	plan: ScrapePlan;
	understood: boolean;
	onUnderstood: (understood: boolean) => void;
	onStart: () => void;
	pending: boolean;
	failure: ReactNode;
}) {
	return (
		<>
			<div className="flex items-center justify-between">
				<DialogTitle>Dry run, sem custo</DialogTitle>
				<Chip tone={plan.overLimit ? "bad" : "ok"}>{plan.overLimit ? "Acima do limite" : "Nada foi executado"}</Chip>
			</div>
			<DialogDescription className="sr-only">Estimativa da próxima execução</DialogDescription>
			<dl className="m-0 grid grid-cols-[150px_1fr] gap-x-3 gap-y-2">
				<dt className="text-mute">Pesquisas</dt>
				<dd className="m-0">
					{plan.requests.map((r) => (
						<div key={r.location}>
							{r.terms.join(", ")} em {r.location}
						</div>
					))}
				</dd>
				<dt className="text-mute">Lugares, no máximo</dt>
				<dd className="m-0 font-mono">{plan.maxPlaces}</dd>
				<dt className="text-mute">Custo máximo</dt>
				<dd className="m-0 font-mono font-semibold">{estimate(plan.estimatedMaxUsd)} Apify</dd>
			</dl>
			{plan.overLimit && (
				<>
					<div className="rounded-md bg-warn-soft px-3 py-2.5 text-warn">
						Esta execução pode trazer mais lugares do que o limite por execução.
					</div>
					<label className="flex items-start gap-2.5">
						<input
							type="checkbox"
							className="mt-[3px]"
							checked={understood}
							onChange={() => onUnderstood(!understood)}
						/>
						Percebo que vou executar com --allow-over-limit e gastar até {estimate(plan.estimatedMaxUsd)}.
					</label>
				</>
			)}
			{failure}
			<div className="flex justify-end gap-2">
				<DialogClose asChild>
					<Button>Cancelar</Button>
				</DialogClose>
				<Button variant="primary" disabled={pending || (plan.overLimit && !understood)} onClick={onStart}>
					{plan.overLimit ? "Executar com --allow-over-limit" : "Executar"}{" "}
					<Cost onAccent>{estimate(plan.estimatedMaxUsd)}</Cost>
				</Button>
			</div>
		</>
	);
}

function EnrichBody({
	plan,
	onStart,
	pending,
	failure,
}: {
	plan: EnrichPlan;
	onStart: () => void;
	pending: boolean;
	failure: ReactNode;
}) {
	const leads = Math.min(plan.pending, plan.batch);
	return (
		<>
			<div className="flex items-center justify-between">
				<DialogTitle>Enriquecer leads qualificados</DialogTitle>
				<Chip tone="ok">Nada foi executado</Chip>
			</div>
			<DialogDescription className="sr-only">Estimativa do próximo enriquecimento</DialogDescription>
			<dl className="m-0 grid grid-cols-[150px_1fr] gap-x-3 gap-y-2">
				<dt className="text-mute">À espera</dt>
				<dd className="m-0 font-mono">{plan.pending} leads</dd>
				<dt className="text-mute">Este lote</dt>
				<dd className="m-0 font-mono">{leads} leads</dd>
				<dt className="text-mute">Reviews por lead</dt>
				<dd className="m-0 font-mono">até {plan.maxReviews}</dd>
			</dl>
			<div className="text-mute">Website, reviews no Apify e análise no LLM. O custo aparece no histórico.</div>
			{failure}
			<div className="flex justify-end gap-2">
				<DialogClose asChild>
					<Button>Cancelar</Button>
				</DialogClose>
				<Button variant="primary" disabled={pending || leads === 0} onClick={onStart}>
					Enriquecer {leads} leads
				</Button>
			</div>
		</>
	);
}
