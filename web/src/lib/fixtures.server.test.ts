import { describe, expect, it } from "vitest";
import { fetchCampaign, fetchCampaigns } from "#/features/campaigns/api.server";
import { fetchCompany } from "#/features/company/api.server";
import { fetchLead } from "#/features/leads/api.server";
import { fetchCampaignRuns } from "#/features/runs/api.server";
import { fetchUsage } from "#/features/usage/api.server";
import { NotFoundError } from "./fake-api";

// Every fake endpoint parses its fixtures through the response schema, so a fixture
// that drifts from the schema fails here rather than in the browser.
describe("fake API", () => {
	it("serves fixtures that match the response schemas", async () => {
		await expect(fetchCampaigns()).resolves.toHaveLength(5);
		await expect(fetchCompany()).resolves.toMatchObject({ name: "Raposa Software, Lda." });
		await expect(fetchUsage("2026-09")).resolves.toMatchObject({ month: "2026-09", apifyUsd: 5.2 });
		// Without a month the fixtures open on their newest one, whatever today's date is.
		await expect(fetchUsage()).resolves.toMatchObject({ month: "2026-10" });
	});

	it("returns runs newest first", async () => {
		const runs = await fetchCampaignRuns("clinicas-talatona");
		expect(runs.map((r) => r.id)).toEqual(["run_0234", "run_0231", "run_0230", "run_0229"]);
	});

	it("throws NotFoundError for unknown ids", async () => {
		await expect(fetchLead("nope")).rejects.toBeInstanceOf(NotFoundError);
		await expect(fetchCampaign("nope")).rejects.toBeInstanceOf(NotFoundError);
	});
});
