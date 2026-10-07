import type { BackendLead } from "#/lib/api-contract";
import type { Lead } from "./schema";

/** Portuguese names for the complaint kinds the backend stores (Stage2Scorer.COMPLAINT_KINDS). */
const COMPLAINT_THEMES: Record<string, string> = {
	contact: "Difícil de contactar",
	booking: "Marcação difícil",
	waiting: "Demora no atendimento",
};

/**
 * What the website crawl found, one row per check. The facts come from the
 * API; this only words them. A place with no site of its own has nothing to
 * crawl, so the row says why.
 */
export function auditOf(b: BackendLead): Lead["audit"] {
	if (b.websiteKind === "NONE") {
		return [
			{ check: "Website", result: "FAIL", detail: "Não existe. O Google Maps não tem ligação para nenhum site." },
		];
	}
	if (b.websiteKind === "SOCIAL_ONLY") {
		return [{ check: "Website", result: "FAIL", detail: "Só tem uma página de rede social, sem site próprio." }];
	}
	const crawl = b.websiteCrawl;
	if (crawl === null) {
		return [{ check: "Website", result: "PENDING", detail: "Ainda não analisado. Surge ao enriquecer a campanha." }];
	}
	if (!crawl.reachable) {
		return [{ check: "Carrega", result: "FAIL", detail: crawl.error ?? "O site não responde." }];
	}
	const rows: Lead["audit"] = [{ check: "Carrega", result: "OK", detail: "O site responde." }];
	if (crawl.https !== null) {
		rows.push(
			crawl.https
				? { check: "HTTPS", result: "OK", detail: "Liga por HTTPS." }
				: { check: "HTTPS", result: "FAIL", detail: "Liga por HTTP simples, sem HTTPS." },
		);
	}
	if (crawl.mobileFriendly !== null) {
		rows.push(
			crawl.mobileFriendly
				? { check: "Telemóvel", result: "OK", detail: "Adapta-se ao ecrã do telemóvel." }
				: { check: "Telemóvel", result: "FAIL", detail: "Sem viewport nem layout adaptável." },
		);
	}
	if (crawl.stale !== null) {
		rows.push(
			crawl.stale
				? { check: "Actualizado", result: "WARN", detail: "Direitos de autor com pelo menos dois anos." }
				: { check: "Actualizado", result: "OK", detail: "Conteúdo recente." },
		);
	}
	return rows;
}

/** Null until stage 2 ran. The API stores the complaint kinds, not counts or quotes. */
export function complaintsOf(b: BackendLead): Lead["complaints"] {
	if (b.stage2Breakdown === null) return null;
	return b.complaintKinds.map((kind) => ({ theme: COMPLAINT_THEMES[kind] ?? kind, mentions: null, quotes: [] }));
}
