import { fakeResponse, NotFoundError } from "#/lib/fake-api";
import { LEADS } from "./fixtures";
import { DAILY_GOAL } from "./model";
import { Lead, LeadList } from "./schema";

/** All leads, ranked first and excluded last. Becomes GET /api/leads. */
export async function fetchLeads(): Promise<Lead[]> {
	const ranked = LEADS.filter((l) => l.rank !== null).sort((a, b) => (a.rank ?? 0) - (b.rank ?? 0));
	const excluded = LEADS.filter((l) => l.rank === null);
	return fakeResponse(LeadList, [...ranked, ...excluded]);
}

/** Becomes GET /api/leads/{id}. */
export async function fetchLead(id: string): Promise<Lead> {
	const lead = LEADS.find((l) => l.id === id);
	if (!lead) throw new NotFoundError(`lead ${id} not found`);
	return fakeResponse(Lead, lead);
}

/** The daily contact queue: qualified leads nobody has contacted yet. Planned step 4. */
export async function fetchTodayQueue(): Promise<Lead[]> {
	const leads = await fetchLeads();
	return leads.filter((l) => l.stage === "QUALIFIED" && l.status === "NEW").slice(0, DAILY_GOAL);
}
