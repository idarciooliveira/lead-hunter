import { apiBaseUrl } from "#/lib/api-config";
import { BackendUsage, BackendUsageEntries } from "#/lib/api-contract";
import { fakeResponse } from "#/lib/fake-api";
import { apiFetch } from "#/lib/http";
import { USAGE } from "./fixtures";
import { UsageMonth } from "./schema";

/** How many runs and calls the month shows, newest first. */
const ENTRIES = 30;

/** The month pages open on: this month in Luanda, or the newest fixture month without an API. */
export function currentUsageMonth(): string {
	if (apiBaseUrl() === null) return USAGE[0].month;
	return new Intl.DateTimeFormat("en-CA", { timeZone: "Africa/Luanda", year: "numeric", month: "2-digit" }).format(
		new Date(),
	);
}

/** GET /api/usage?month= and GET /api/usage/entries?month=, as one month. */
export async function fetchUsage(month: string): Promise<UsageMonth> {
	if (apiBaseUrl() === null) {
		const empty = { month, budgetUsd: USAGE[0].budgetUsd, apifyUsd: 0, llmUsd: 0, byCampaign: [], events: [] };
		return fakeResponse(UsageMonth, USAGE.find((m) => m.month === month) ?? empty);
	}
	const query = `month=${encodeURIComponent(month)}`;
	const [summary, entries] = await Promise.all([
		apiFetch(BackendUsage, `/usage?${query}`),
		apiFetch(BackendUsageEntries, `/usage/entries?${query}&limit=${ENTRIES}`),
	]);
	return fakeResponse(UsageMonth, {
		month,
		budgetUsd: summary.budgetUsd,
		apifyUsd: summary.apify.costUsd,
		llmUsd: summary.llm.costUsd,
		byCampaign: summary.byCampaign.map((c) => ({ campaign: c.slug, usd: c.apifyUsd + c.llmUsd })),
		events: entries.map((e) => ({
			at: e.at,
			campaign: e.campaignSlug,
			source: e.kind === "apify" ? "APIFY" : "LLM",
			detail: e.label,
			costUsd: e.costUsd,
		})),
	});
}
