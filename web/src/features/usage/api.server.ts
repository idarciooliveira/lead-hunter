import { CAMPAIGNS } from "#/features/campaigns/fixtures";
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

/** The name a fixture event or total carries for a campaign slug, or the slug itself when no fixture campaign has it. */
function fixtureName(campaign: string): string {
	return CAMPAIGNS.find((c) => c.slug === campaign)?.name ?? campaign;
}

/**
 * One fixture month narrowed to a campaign. Its total is the campaign's line in the monthly breakdown, so the page
 * agrees with itself. The fixtures do not split a campaign's spend by source, so the split follows the month's own
 * Apify and LLM mix. The events list still only shows the runs and calls the fixture keeps.
 */
function forCampaign(m: UsageMonth, campaign: string): UsageMonth {
	const name = fixtureName(campaign);
	const usd = m.byCampaign.find((c) => c.campaign === name)?.usd ?? 0;
	const monthUsd = m.apifyUsd + m.llmUsd;
	const apifyUsd = monthUsd === 0 ? 0 : usd * (m.apifyUsd / monthUsd);
	return {
		...m,
		apifyUsd,
		llmUsd: usd - apifyUsd,
		byCampaign: m.byCampaign.filter((c) => c.campaign === name),
		events: m.events.filter((e) => e.campaign === name),
	};
}

/**
 * GET /api/usage?month=&campaign= and GET /api/usage/entries?month=&campaign=, as one month; this month by default.
 * Without `campaign` the month covers every campaign.
 */
export async function fetchUsage(month: string = currentMonth(), campaign?: string): Promise<UsageMonth> {
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
		const found = USAGE.find((m) => m.month === month) ?? empty;
		return fakeResponse(UsageMonth, campaign ? forCampaign(found, campaign) : found);
	}
	const query = `month=${encodeURIComponent(month)}${campaign ? `&campaign=${encodeURIComponent(campaign)}` : ""}`;
	const [summary, entries] = await Promise.all([
		apiFetch(BackendUsage, `/usage?${query}`),
		apiFetch(BackendUsageEntries, `/usage/entries?${query}&limit=${ENTRIES}`),
	]);
	return fakeResponse(UsageMonth, {
		month,
		budgetUsd: summary.budgetUsd,
		apifyUsd: summary.apify.costUsd,
		llmUsd: summary.llm.costUsd,
		// The reservation covers the whole organisation, so it only means something next to the whole month.
		committedUsd: month === utcMonth() && !campaign ? summary.committedUsd : null,
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
