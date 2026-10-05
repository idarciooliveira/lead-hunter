import { useState } from "react";
import { Button } from "#/components/ui/button";
import { Chip } from "#/components/ui/chip";
import { Cost } from "#/components/ui/cost";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogTitle } from "#/components/ui/dialog";
import { estimate, usd } from "#/lib/format";
import type { Campaign } from "../schema";

export type RunDialog = "dry" | "over" | null;

/**
 * The confirmation flow from ADR 0033: a free dry run first, then an explicit
 * opt-in when the run would pass the campaign's cost limit.
 */
export function RunDialogs({
	campaign,
	open,
	onOpenChange,
}: {
	campaign: Campaign;
	open: RunDialog;
	onOpenChange: (open: RunDialog) => void;
}) {
	const [understood, setUnderstood] = useState(false);
	const run = campaign.nextRun;
	const after = campaign.spendUsd + run.costUsd;
	const over = after - campaign.limitUsd;
	const close = () => {
		setUnderstood(false);
		onOpenChange(null);
	};

	return (
		<>
			<Dialog open={open === "dry"} onOpenChange={(o) => !o && close()}>
				<DialogContent>
					<div className="flex items-center justify-between">
						<DialogTitle>Dry run, sem custo</DialogTitle>
						<Chip tone="ok">Nada foi executado</Chip>
					</div>
					<DialogDescription className="sr-only">Estimativa da próxima execução</DialogDescription>
					<dl className="m-0 grid grid-cols-[150px_1fr] gap-x-3 gap-y-2">
						<dt className="text-mute">Pesquisa</dt>
						<dd className="m-0">{run.query}</dd>
						<dt className="text-mute">Lugares estimados</dt>
						<dd className="m-0 font-mono">≈ {run.places}</dd>
						<dt className="text-mute">Custo estimado</dt>
						<dd className="m-0 font-mono font-semibold">{estimate(run.costUsd)} Apify</dd>
						<dt className="text-mute">Gasto da campanha</dt>
						<dd className="m-0 font-mono">
							{usd(campaign.spendUsd)} + {usd(run.costUsd)} = {usd(after)}
						</dd>
						<dt className="text-mute">Limite</dt>
						<dd className="m-0 font-mono">{usd(campaign.limitUsd)}</dd>
					</dl>
					{over > 0 && (
						<div className="rounded-md bg-warn-soft px-3 py-2.5 text-warn">
							Esta execução passa o limite da campanha em {usd(over)}.
						</div>
					)}
					<div className="flex justify-end gap-2">
						<DialogClose asChild>
							<Button>Cancelar</Button>
						</DialogClose>
						<Button variant="primary" onClick={() => (over > 0 ? onOpenChange("over") : close())}>
							Continuar <Cost onAccent>{estimate(run.costUsd)}</Cost>
						</Button>
					</div>
				</DialogContent>
			</Dialog>

			<Dialog open={open === "over"} onOpenChange={(o) => !o && close()}>
				<DialogContent className="border-bad">
					<div className="flex items-center justify-between">
						<DialogTitle>Passar o limite da campanha?</DialogTitle>
						<Chip tone="bad">Acima do limite</Chip>
					</div>
					<DialogDescription>
						O limite de {usd(campaign.limitUsd)} foi definido por ti. Executar com{" "}
						<span className="font-mono text-tx">--allow-over-limit</span> gasta até {usd(over)} a mais e conta para o
						orçamento de $10 do mês.
					</DialogDescription>
					<label className="flex items-start gap-2.5">
						<input
							type="checkbox"
							className="mt-[3px]"
							checked={understood}
							onChange={() => setUnderstood(!understood)}
						/>
						Percebo que vou gastar {usd(after)} numa campanha com limite de {usd(campaign.limitUsd)}.
					</label>
					<div className="flex justify-end gap-2">
						<DialogClose asChild>
							<Button>Cancelar</Button>
						</DialogClose>
						<Button variant="primary" disabled={!understood} onClick={close}>
							Executar com --allow-over-limit <Cost onAccent>{estimate(run.costUsd)}</Cost>
						</Button>
					</div>
				</DialogContent>
			</Dialog>
		</>
	);
}
