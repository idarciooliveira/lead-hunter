import { useState } from "react";
import { CallLink, WhatsAppLink } from "#/components/contact-actions";
import { Button } from "#/components/ui/button";
import { Card, CardHeader } from "#/components/ui/card";
import { Chip, PlannedChip, type Tone } from "#/components/ui/chip";
import { Cost } from "#/components/ui/cost";
import { Label, NativeSelect, Textarea } from "#/components/ui/field";
import { ProgressBar } from "#/components/ui/progress";
import { Tooltip } from "#/components/ui/tooltip";
import { rating } from "#/lib/format";
import {
	bandOf,
	contactOf,
	LOST_REASONS,
	NOT_NOW_REASONS,
	OUTCOMES,
	type Outcome,
	outcomeOf,
	points,
	scoreSummary,
} from "../model";
import { useMarkLead } from "../queries";
import type { AuditResult, Lead, LostReason, ScoreLine } from "../schema";
import { AreaMap } from "./area-map";
import { StageChip } from "./status-chips";

export function LeadHero({ lead }: { lead: Lead }) {
	const site = lead.website ? `${lead.website.host}${lead.website.https ? "" : " (sem HTTPS)"}` : "Sem website";
	return (
		<Card className="flex flex-wrap items-stretch gap-6 p-4 md:p-5">
			<div className="flex min-w-0 flex-[1_1_380px] flex-col gap-3">
				<div className="flex flex-wrap items-center gap-2">
					<StageChip stage={lead.stage} />
					<Chip>{lead.category}</Chip>
					<Chip>{lead.area}</Chip>
					<span className="font-mono text-mute">
						#{lead.rank ?? "–"} · {lead.id}
					</span>
				</div>
				<h1 className="m-0 text-[22px] font-semibold tracking-[-0.01em] md:text-2xl">{lead.name}</h1>
				<dl className="m-0 grid grid-cols-[96px_1fr] gap-x-3 gap-y-1.5">
					<dt className="text-mute">Endereço</dt>
					<dd className="m-0">{lead.address}</dd>
					<dt className="text-mute">Telefone</dt>
					<dd className="m-0 font-mono">{lead.phone ?? "Sem telefone"}</dd>
					<dt className="text-mute">Website</dt>
					<dd className="m-0">{site}</dd>
					<dt className="text-mute">Google Maps</dt>
					<dd className="m-0 font-mono">
						{rating(lead.rating)} estrelas · {lead.reviewCount} avaliações
					</dd>
				</dl>
				<Tooltip content={scoreSummary(lead)}>
					{/* biome-ignore lint/a11y/noNoninteractiveTabindex: focus shows the explanation to keyboard users. */}
					<div tabIndex={0} className="mt-1 hidden w-fit cursor-help items-center gap-4 md:flex">
						<span className="font-mono text-[32px] font-semibold tracking-[-0.02em]">{lead.score ?? "–"}</span>
						<div className="w-40">
							<ProgressBar value={lead.score ?? 0} tone={bandOf(lead.score)} label="Pontuação" />
							<div className="mt-0.5 text-[11px] text-mute">de 100 · passa o rato para ver porquê</div>
						</div>
					</div>
				</Tooltip>
				<div className="flex flex-col gap-2 md:hidden">
					<div className="flex items-center gap-3">
						<span className="font-mono text-[30px] font-semibold">{lead.score ?? "–"}</span>
						<ProgressBar value={lead.score ?? 0} tone={bandOf(lead.score)} label="Pontuação" className="flex-1" />
					</div>
					<details>
						<summary className="flex min-h-11 cursor-pointer items-center font-medium text-acc-tx">
							Porquê {lead.score ?? "excluído"}?
						</summary>
						<div className="text-mute">{scoreSummary(lead)}</div>
					</details>
					<div className="flex gap-2">
						<WhatsAppLink phone={lead.phone} message={lead.pitch} size="touch" className="flex-1">
							Abrir WhatsApp
						</WhatsAppLink>
						<CallLink phone={lead.phone} size="touch-icon" label={false} />
					</div>
				</div>
			</div>
			<AreaMap />
		</Card>
	);
}

function ScoreLines({ title, lines }: { title: string; lines: ScoreLine[] }) {
	return (
		<>
			<div className="eyebrow mt-4 mb-1 flex justify-between first:mt-0">
				<span>{title}</span>
				<span className="font-mono">{points(lines)} pontos</span>
			</div>
			{lines.map((line) => (
				<div key={line.reason} className="flex items-center gap-3 border-b border-line py-2">
					<span className="w-9 font-mono font-semibold text-ok">+{line.points}</span>
					<span>{line.reason}</span>
				</div>
			))}
		</>
	);
}

export function ScoreBreakdown({ lead }: { lead: Lead }) {
	return (
		<Card>
			<CardHeader
				title="Decomposição da pontuação"
				aside={<span className="font-mono font-semibold">{lead.score ?? "–"}/100</span>}
			/>
			<div className="px-4 py-3">
				{lead.stage === "EXCLUDED" ? (
					<div className="text-mute">{scoreSummary(lead)}</div>
				) : (
					<>
						<ScoreLines title="Stage 1 · Google Maps" lines={lead.breakdown.stage1} />
						{lead.breakdown.stage2 ? (
							<ScoreLines title="Stage 2 · Website e reviews" lines={lead.breakdown.stage2} />
						) : (
							<div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-md border border-dashed border-line2 p-3">
								<span className="text-mute">Stage 2 ainda não corre para este lead.</span>
								<Button size="sm">
									Enriquecer <Cost>≈ $0.004</Cost>
								</Button>
							</div>
						)}
					</>
				)}
			</div>
		</Card>
	);
}

const AUDIT: Record<AuditResult, { label: string; tone: Tone }> = {
	OK: { label: "OK", tone: "ok" },
	WARN: { label: "Aviso", tone: "warn" },
	FAIL: { label: "Falha", tone: "bad" },
	PENDING: { label: "Pendente", tone: "neutral" },
};

export function WebsiteAudit({ lead }: { lead: Lead }) {
	return (
		<Card>
			<CardHeader title="Auditoria do website" />
			<div className="px-4 pt-1 pb-3">
				{lead.audit.map((a) => (
					<div key={a.check} className="flex flex-wrap items-center gap-3 border-b border-line py-2 last:border-b-0">
						<Chip tone={AUDIT[a.result].tone} className="w-[58px] justify-center">
							{AUDIT[a.result].label}
						</Chip>
						<span className="w-[130px] font-medium">{a.check}</span>
						<span className="text-mute">{a.detail}</span>
					</div>
				))}
			</div>
		</Card>
	);
}

export function ReviewComplaints({ lead }: { lead: Lead }) {
	const complaints = lead.complaints;
	const note =
		complaints === null
			? "As reviews ainda não foram analisadas."
			: complaints.length
				? "Temas mais repetidos nas últimas 60 reviews."
				: "Sem queixas recorrentes nas últimas 60 reviews.";
	const quotes = (complaints ?? []).flatMap((c) => c.quotes).slice(0, 2);
	return (
		<Card>
			<CardHeader title="Queixas nas avaliações" />
			<div className="flex flex-col gap-3 px-4 py-3">
				<div className="text-mute">{note}</div>
				{complaints && complaints.length > 0 && (
					<div className="flex flex-wrap gap-1.5">
						{complaints.slice(0, 4).map((c) => (
							<Chip key={c.theme} tone="warn">
								{c.theme} · {c.mentions} menções
							</Chip>
						))}
					</div>
				)}
				{quotes.map((q) => (
					<blockquote key={q} className="m-0 rounded-md border border-line bg-bg px-3 py-2.5">
						{q}
						<div className="mt-1 text-[11px] text-mute">Avaliação no Google Maps</div>
					</blockquote>
				))}
			</div>
		</Card>
	);
}

export function SuggestedMessage({ lead }: { lead: Lead }) {
	return (
		<Card>
			<CardHeader title="Mensagem sugerida" aside={<PlannedChip step={4} />} />
			<div className="flex flex-col gap-3 px-4 py-3">
				<div className="whitespace-pre-wrap rounded-md border border-line bg-bg p-3">{lead.pitch}</div>
				<div className="flex flex-wrap gap-2">
					<WhatsAppLink phone={lead.phone} message={lead.pitch} size="default" shortcut="W">
						Abrir WhatsApp
					</WhatsAppLink>
					<Button>
						Regenerar <Cost>≈ $0.002</Cost>
					</Button>
				</div>
			</div>
		</Card>
	);
}

/**
 * Outcome buttons and notes, saved through PATCH /api/leads/{id} (ADR 0012,
 * 0020). Outcomes without a reason save at once; "Perdido" asks for one of the
 * four terminal reasons and "Não agora" for its note first. Backend failures
 * render verbatim, mirroring the CLI's `error: <message>`.
 */
export function ContactOutcome({ lead }: { lead: Lead }) {
	const mark = useMarkLead();
	const saved = outcomeOf(lead.status, lead.lostReason);
	const contact = contactOf(lead.status, lead.lostReason);
	const [pending, setPending] = useState<Outcome | null>(null);
	const [lostReason, setLostReason] = useState<LostReason>("NOT_INTERESTED");
	const [note, setNote] = useState(lead.note ?? "");
	const active = pending ?? saved;

	const save = (status: Lead["status"], reason: LostReason | null) =>
		mark.mutate(
			{ id: lead.id, input: { status, lostReason: reason, note: note || null } },
			{ onSuccess: () => setPending(null) },
		);

	return (
		<Card>
			<CardHeader title="Resultado do contacto" aside={<Chip tone={contact.tone}>{contact.label}</Chip>} />
			<div className="flex flex-col gap-3 px-4 py-3">
				<div className="flex flex-wrap gap-1.5">
					{OUTCOMES.map((o) => (
						<Button
							key={o.value}
							size="sm"
							variant={active === o.value ? "active" : "default"}
							aria-pressed={active === o.value}
							disabled={mark.isPending}
							onClick={() => (o.value === "LOST" || o.value === "NOT_NOW" ? setPending(o.value) : save(o.value, null))}
						>
							{o.label}
						</Button>
					))}
				</div>
				{pending === "LOST" && (
					<div>
						<Label htmlFor="lost-reason">Motivo da perda</Label>
						<NativeSelect
							id="lost-reason"
							value={lostReason}
							onChange={(e) => setLostReason(e.target.value as LostReason)}
						>
							{LOST_REASONS.map((r) => (
								<option key={r.value} value={r.value}>
									{r.label}
								</option>
							))}
						</NativeSelect>
					</div>
				)}
				{pending === "NOT_NOW" && (
					<div>
						<Label htmlFor="not-now-reason">Motivo de "Não agora"</Label>
						<NativeSelect id="not-now-reason" value={note} onChange={(e) => setNote(e.target.value)}>
							{note !== "" && !NOT_NOW_REASONS.includes(note) && <option value={note}>{note}</option>}
							{NOT_NOW_REASONS.map((r) => (
								<option key={r} value={r}>
									{r}
								</option>
							))}
						</NativeSelect>
					</div>
				)}
				{pending !== null && (
					<div className="flex gap-2">
						<Button
							size="sm"
							variant="primary"
							disabled={mark.isPending}
							onClick={() => save("LOST", pending === "NOT_NOW" ? "NOT_NOW" : lostReason)}
						>
							Guardar
						</Button>
						<Button size="sm" variant="ghost" onClick={() => setPending(null)}>
							Cancelar
						</Button>
					</div>
				)}
				<div>
					<Label htmlFor="lead-note">Notas</Label>
					<Textarea id="lead-note" value={note} onChange={(e) => setNote(e.target.value)} />
				</div>
				{mark.isError && (
					<div role="alert" className="text-sm text-bad">
						{mark.error.message}
					</div>
				)}
			</div>
		</Card>
	);
}
