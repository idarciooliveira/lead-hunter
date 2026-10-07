import { apiBaseUrl } from "#/lib/api-config.server";
import {
	BackendCampaignList,
	BackendLead,
	BackendLeadList,
	type BackendLead as BackendLeadType,
} from "#/lib/api-contract";
import { fakeResponse, NotFoundError } from "#/lib/fake-api";
import { apiFetch, apiMutate } from "#/lib/http.server";
import { auditOf, complaintsOf } from "./enrichment";
import { LEADS } from "./fixtures";
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

/**
 * The daily contact queue: GET /api/leads/today (ADR 0041). The size comes
 * from the company's weekly capacity, so the web never cuts the queue itself.
 * Without an API the fixtures stand in, filtered the same way.
 */
export async function fetchTodayQueue(): Promise<Lead[]> {
	if (apiBaseUrl() === null) {
		const leads = await fetchLeads();
		return fakeResponse(
			LeadList,
			leads.filter((l) => l.stage === "QUALIFIED" && l.status === "NEW"),
		);
	}
	const queue = await apiFetch(BackendLeadList, "/leads/today");
	return fakeResponse(LeadList, rankBackendLeads(queue));
}

/**
 * Writes a new pitch for the lead: POST /api/leads/{id}/pitch (ADR 0040). Without an
 * API configured the sample pitch stays, so the smoke build keeps working.
 */
export async function regeneratePitch(id: string): Promise<Lead> {
	if (apiBaseUrl() === null) return fetchLead(id);
	const updated = await apiMutate(BackendLead, `/leads/${encodeURIComponent(id)}/pitch`, "POST", {});
	return fakeResponse(Lead, toLead(updated, null));
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
 * returns them; the UI never computes them (ADR 0007). The audit and the
 * complaints come with the single-lead endpoints only; a list row has no
 * crawl, so its audit says "not analysed".
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
		mapsUrl: b.mapsUrl,
		rating: b.rating ?? 0,
		reviewCount: b.reviewsCount,
		phone: b.phoneE164,
		website: websiteLink(b.website),
		stage: b.stage,
		stageReason: b.stageReason,
		status: b.status,
		lostReason: b.lostReason,
		score: b.score,
		breakdown: {
			stage1: b.breakdown.map((i) => ({ points: i.points, reason: i.reason })),
			stage2: b.stage2Breakdown?.map((i) => ({ points: i.points, reason: i.reason })) ?? null,
		},
		audit: auditOf(b),
		complaints: complaintsOf(b),
		pitch: b.pitch ?? "",
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
