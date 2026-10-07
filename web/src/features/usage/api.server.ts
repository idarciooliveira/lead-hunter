import { apiBaseUrl } from "#/lib/api-config.server";
import { BackendUsage, BackendUsageEntries } from "#/lib/api-contract";
import { fakeResponse } from "#/lib/fake-api";
import { apiFetch } from "#/lib/http.server";
import { USAGE } from "./fixtures";
import { UsageMonth } from "./schema";

/** How many runs and calls the month shows, newest first. */
const ENTRIES = 30;

/** The month pages open on: this month in Luanda, or the newest fixture month without an API. */
function currentMonth(): string {
	if (apiBaseUrl() === null) return USAGE[0].month;
	return new Intl.DateTimeFormat("en-CA", { timeZone: "Africa/Luanda", year: "numeric", month: "2-digit" }).format(
		new Date(),
	);
}

/** The month the API's committedUsd covers. */
function utcMonth(): string {
	return new Date().toISOString().slice(0, 7);
}

/** GET /api/usage?month= and GET /api/usage/entries?month=, as one month; this month by default. */
export async function fetchUsage(month: string = currentMonth()): Promise<UsageMonth> {
	if (apiBaseUrl() === null) {
		const empty = {
			month,
			budgetUsd: USAGE[0].budgetUsd,
			apifyUsd: 0,
			llmUsd: 0,
			committedUsd: null,
			byCampaign: [],
			events: [],
		};
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
		committedUsd: month === utcMonth() ? summary.committedUsd : null,
		byCampaign: summary.byCampaign.map((c) => ({
			campaign: c.slug ?? "Sem campanha ou apagada",
			usd: c.apifyUsd + c.llmUsd,
		})),
		events: entries.map((e) => ({
			at: e.at,
			campaign: e.campaignSlug,
			source: e.kind === "apify" ? "APIFY" : "LLM",
			detail: e.label,
			costUsd: e.costUsd,
		})),
	});
}
