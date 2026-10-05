import { apiBaseUrl } from "#/lib/api-config.server";
import {
	BackendCampaignList,
	BackendLead,
	BackendLeadList,
	type BackendLead as BackendLeadType,
} from "#/lib/api-contract";
import { fakeResponse, NotFoundError } from "#/lib/fake-api";
import { apiFetch, apiMutate } from "#/lib/http.server";
import { LEADS } from "./fixtures";
import { DAILY_GOAL } from "./model";
import { Lead, LeadList, type LeadStatus, type Lead as LeadType, type LostReason } from "./schema";

/** All leads, ranked first and excluded last. Becomes GET /api/leads. */
export async function fetchLeads(): Promise<Lead[]> {
	if (apiBaseUrl() === null) {
		const ranked = LEADS.filter((l) => l.rank !== null).sort((a, b) => (a.rank ?? 0) - (b.rank ?? 0));
		const excluded = LEADS.filter((l) => l.rank === null);
		return fakeResponse(LeadList, [...ranked, ...excluded]);
	}
	const campaigns = await apiFetch(BackendCampaignList, "/campaigns");
	const lists = await Promise.all(
		campaigns.map((c) =>
			apiFetch(BackendLeadList, `/campaigns/${encodeURIComponent(c.slug)}/leads?stage=ALL&limit=200`),
		),
	);
	return fakeResponse(LeadList, rankBackendLeads(lists.flat()));
}

/** Becomes GET /api/leads/{id}. */
export async function fetchLead(id: string): Promise<Lead> {
	if (apiBaseUrl() === null) {
		const lead = LEADS.find((l) => l.id === id);
		if (!lead) throw new NotFoundError(`lead ${id} not found`);
		return fakeResponse(Lead, lead);
	}
	const lead = await apiFetch(BackendLead, `/leads/${encodeURIComponent(id)}`);
	// Rank is list-scoped (position among non-excluded leads), so a single fetch leaves it empty.
	return fakeResponse(Lead, toLead(lead, null));
}

/** The daily contact queue: qualified leads nobody has contacted yet. No endpoint: a filter over the mapped API leads. */
export async function fetchTodayQueue(): Promise<Lead[]> {
	const leads = await fetchLeads();
	return leads.filter((l) => l.stage === "QUALIFIED" && l.status === "NEW").slice(0, DAILY_GOAL);
}

export type MarkLeadInput = { status: LeadStatus; lostReason?: LostReason | null; note?: string | null };

/**
 * Marks a contact outcome: PATCH /api/leads/{id} (ADR 0012, 0020). Without an
 * API configured the outcome lands on the in-memory fixtures for the session,
 * so the pages keep working in the smoke build.
 */
export async function markLead(id: string, input: MarkLeadInput): Promise<Lead> {
	if (apiBaseUrl() === null) {
		const lead = LEADS.find((l) => l.id === id);
		if (!lead) throw new NotFoundError(`lead ${id} not found`);
		if (input.status === "LOST") lead.lostReason = input.lostReason ?? null;
		else lead.lostReason = null;
		lead.status = input.status;
		// Like the backend (LeadRepository.updateOutcome): a blank note is no note.
		lead.note = typeof input.note === "string" && input.note.trim() !== "" ? input.note : null;
		return fakeResponse(Lead, lead);
	}
	const updated = await apiMutate(BackendLead, `/leads/${encodeURIComponent(id)}`, "PATCH", {
		status: input.status,
		lostReason: input.lostReason ?? null,
		note: input.note ?? null,
	});
	// Rank is list-scoped (position among non-excluded leads), so a single mark leaves it empty.
	return fakeResponse(Lead, toLead(updated, null));
}

/**
 * Backend rows become UI leads. Scores and reasons are data, as the API
 * returns them; the UI never computes them (ADR 0007). Fields the API does
 * not serve yet (stage-2 audit, complaints, pitch) stay empty until their
 * endpoints land in docs/api.md.
 */
export function toLead(b: BackendLeadType, rank: number | null): LeadType {
	return {
		id: String(b.id),
		campaignSlug: b.campaignSlug,
		rank,
		name: b.name,
		category: b.category ?? "",
		area: b.neighborhood ?? "",
		address: b.address ?? "",
		rating: b.rating ?? 0,
		reviewCount: b.reviewsCount,
		phone: b.phoneE164,
		website: websiteLink(b.website),
		stage: b.stage,
		stageReason: b.stageReason,
		status: b.status,
		lostReason: b.lostReason,
		score: b.score,
		breakdown: { stage1: b.breakdown.map((i) => ({ points: i.points, reason: i.reason })), stage2: null },
		audit: [],
		complaints: null,
		pitch: "",
		note: b.note,
	};
}

/** Best score first, excluded last; the rank is the position among the non-excluded. */
export function rankBackendLeads(rows: BackendLeadType[]): LeadType[] {
	const ordered = [...rows].sort((a, b) => {
		if ((a.stage === "EXCLUDED") !== (b.stage === "EXCLUDED")) return a.stage === "EXCLUDED" ? 1 : -1;
		return b.score - a.score || b.reviewsCount - a.reviewsCount || a.id - b.id;
	});
	let rank = 0;
	return ordered.map((b) => toLead(b, b.stage === "EXCLUDED" ? null : ++rank));
}

function websiteLink(website: string | null): LeadType["website"] {
	if (!website) return null;
	try {
		const url = new URL(website.includes("://") ? website : `https://${website}`);
		return { host: url.host, https: url.protocol === "https:" };
	} catch {
		return null;
	}
}
