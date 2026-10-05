import type { Tone } from "#/components/ui/chip";
import type { Band } from "#/components/ui/progress";
import type { Lead, LeadStage, LeadStatus, LostReason } from "./schema";

export const STAGE_TONE: Record<LeadStage, Tone> = { QUALIFIED: "ok", BELOW_CUT: "neutral", EXCLUDED: "bad" };

export const STAGE_FILTERS = [
	{ value: "ALL", label: "Todos" },
	{ value: "QUALIFIED", label: "Qualificados" },
	{ value: "BELOW_CUT", label: "Abaixo do corte" },
	{ value: "EXCLUDED", label: "Excluídos" },
] as const;
export type StageFilter = (typeof STAGE_FILTERS)[number]["value"];

/** What the contact buttons set. "Não agora" is LOST with reason NOT_NOW (ADR 0020). */
export type Outcome = Exclude<LeadStatus, "NEW"> | "NOT_NOW";

export const OUTCOMES: { value: Outcome; label: string }[] = [
	{ value: "CONTACTED", label: "Contactado" },
	{ value: "INTERESTED", label: "Respondeu" },
	{ value: "MEETING", label: "Reunião" },
	{ value: "WON", label: "Ganho" },
	{ value: "LOST", label: "Perdido" },
	{ value: "NOT_NOW", label: "Não agora" },
];

const CONTACT: Record<LeadStatus | "NOT_NOW", { label: string; tone: Tone }> = {
	NEW: { label: "Novo", tone: "neutral" },
	CONTACTED: { label: "Contactado", tone: "info" },
	NO_ANSWER: { label: "Sem resposta", tone: "neutral" },
	INTERESTED: { label: "Respondeu", tone: "ok" },
	MEETING: { label: "Reunião", tone: "ok" },
	PROPOSAL_SENT: { label: "Proposta enviada", tone: "ok" },
	WON: { label: "Ganho", tone: "ok" },
	LOST: { label: "Perdido", tone: "bad" },
	NOT_NOW: { label: "Não agora", tone: "warn" },
};

export function contactOf(status: LeadStatus | Outcome, lostReason: LostReason | null = null) {
	return CONTACT[status === "LOST" && lostReason === "NOT_NOW" ? "NOT_NOW" : status];
}

export const NOT_NOW_REASONS = [
	"Sem orçamento este trimestre",
	"Já trata com outro fornecedor",
	"Pediu para voltar em 30 dias",
	"Decisor indisponível",
];

export function bandOf(score: number | null): Band {
	if (score === null) return "lo";
	return score >= 70 ? "hi" : score >= 40 ? "mid" : "lo";
}

/** One-line explanation for tooltips: the three biggest reasons, from the API's breakdown. */
export function scoreSummary(lead: Lead): string {
	if (lead.stage === "EXCLUDED") return `Excluído: ${lead.stageReason ?? "filtro da campanha"}.`;
	const lines = [...lead.breakdown.stage1, ...(lead.breakdown.stage2 ?? [])]
		.sort((a, b) => b.points - a.points)
		.slice(0, 3);
	return `Porquê ${lead.score}: ${lines.map((l) => `+${l.points} ${l.reason.toLowerCase()}`).join("; ")}.`;
}

export function points(lines: { points: number }[] | null): number {
	return (lines ?? []).reduce((sum, line) => sum + line.points, 0);
}

export type LeadFilters = { stage: StageFilter; minScore: number; hasSite: boolean; hasPhone: boolean };

export function filterLeads(leads: Lead[], f: LeadFilters): Lead[] {
	return leads.filter(
		(l) =>
			(f.stage === "ALL" || l.stage === f.stage) &&
			(l.score ?? 0) >= f.minScore &&
			(!f.hasSite || l.website !== null) &&
			(!f.hasPhone || l.phone !== null),
	);
}

export function countByStage(leads: Lead[]): Record<StageFilter, number> {
	return {
		ALL: leads.length,
		QUALIFIED: leads.filter((l) => l.stage === "QUALIFIED").length,
		BELOW_CUT: leads.filter((l) => l.stage === "BELOW_CUT").length,
		EXCLUDED: leads.filter((l) => l.stage === "EXCLUDED").length,
	};
}
