import { describe, expect, it } from "vitest";
import { budgetOf, budgetState, usedOf } from "./model";
import type { UsageMonth } from "./schema";

function month(overrides: Partial<UsageMonth>): UsageMonth {
	return {
		month: "2026-10",
		budgetUsd: 10,
		apifyUsd: 0,
		llmUsd: 0,
		committedUsd: null,
		byCampaign: [],
		events: [],
		...overrides,
	};
}

describe("budgetOf", () => {
	// Campaign A spent 0.5, the organisation spent 9 of its 10.
	const organisation = month({ apifyUsd: 9, llmUsd: 0 });
	const campaign = month({ apifyUsd: 0.5, llmUsd: 0 });

	it("checks the organisation's budget when a campaign narrows the page", () => {
		expect(budgetState(budgetOf(campaign, organisation, "campanha-a"))).toEqual({
			label: "Perto do limite",
			tone: "bad",
		});
		expect(usedOf(budgetOf(campaign, organisation, "campanha-a"))).toBe(9);
	});

	it("checks the page's own month when no campaign is chosen", () => {
		expect(budgetOf(campaign, organisation)).toBe(campaign);
		expect(budgetState(budgetOf(campaign, organisation))).toEqual({ label: "Dentro do orçamento", tone: "ok" });
	});
});
