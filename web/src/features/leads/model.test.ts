import { describe, expect, it } from "vitest";
import { fetchLeads, fetchTodayQueue } from "./api.server";
import { bandOf, contactOf, countByStage, filterLeads, points, scoreSummary } from "./model";

describe("leads model", () => {
	it("explains a score with its three biggest reasons", async () => {
		const [top] = await fetchLeads();
		expect(scoreSummary(top)).toBe(
			"Porquê 80: +25 sem website no google maps; +10 mais de 100 avaliações, clientela activa; +9 sem marcação online e sem website.",
		);
	});

	it("explains an exclusion with the API's reason", async () => {
		const excluded = (await fetchLeads()).find((l) => l.stage === "EXCLUDED");
		expect(excluded && scoreSummary(excluded)).toMatch(/^Excluído: /);
	});

	it("keeps the score equal to its breakdown in the fixtures", async () => {
		for (const lead of await fetchLeads()) {
			if (lead.score === null) continue;
			expect(Math.min(100, points(lead.breakdown.stage1) + points(lead.breakdown.stage2))).toBe(lead.score);
		}
	});

	it("bands scores at 40 and 70", () => {
		expect(bandOf(70)).toBe("hi");
		expect(bandOf(40)).toBe("mid");
		expect(bandOf(39)).toBe("lo");
		expect(bandOf(null)).toBe("lo");
	});

	it("shows LOST with NOT_NOW as Não agora", () => {
		expect(contactOf("LOST", "NOT_NOW").label).toBe("Não agora");
		expect(contactOf("LOST", null).label).toBe("Perdido");
	});

	it("filters by stage, score, website and phone", async () => {
		const leads = await fetchLeads();
		const all = { stage: "ALL", minScore: 0, hasSite: false, hasPhone: false } as const;
		expect(filterLeads(leads, all)).toHaveLength(leads.length);
		expect(filterLeads(leads, { ...all, stage: "EXCLUDED" })).toHaveLength(countByStage(leads).EXCLUDED);
		expect(filterLeads(leads, { ...all, minScore: 70 }).every((l) => (l.score ?? 0) >= 70)).toBe(true);
		expect(filterLeads(leads, { ...all, hasSite: true }).every((l) => l.website)).toBe(true);
		expect(filterLeads(leads, { ...all, hasPhone: true }).every((l) => l.phone)).toBe(true);
	});

	it("queues only new qualified leads, best first", async () => {
		const queue = await fetchTodayQueue();
		expect(queue.length).toBeGreaterThan(0);
		expect(queue.every((l) => l.stage === "QUALIFIED" && l.status === "NEW")).toBe(true);
		expect(queue.map((l) => l.rank)).toEqual([...queue.map((l) => l.rank)].sort((a, b) => (a ?? 0) - (b ?? 0)));
	});
});
