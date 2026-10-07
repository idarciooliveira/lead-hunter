import { afterEach, describe, expect, it, vi } from "vitest";
import type { BackendLead } from "#/lib/api-contract";
import { stubApi } from "#/lib/test-api";
import { fetchLead, fetchLeads, fetchTodayQueue, markLead, regeneratePitch, toLead } from "./api.server";

const QUALIFIED: BackendLead = {
	id: 9001,
	campaignSlug: "mock-clinicas",
	stage: "QUALIFIED",
	status: "NEW",
	lostReason: null,
	note: null,
	score: 65,
	breakdown: [{ code: "MOBILE_PHONE", points: 5, reason: "Telefone móvel" }],
	stage2Breakdown: null,
	stageReason: null,
	name: "Mock Sorriso",
	category: "Clínica",
	address: "Rua 1",
	neighborhood: "Talatona",
	phoneE164: "+244923456789",
	phoneMobile: true,
	website: null,
	websiteKind: "NONE",
	rating: 4.3,
	reviewsCount: 142,
	mapsUrl: "https://maps.example/p1",
	complaintKinds: [],
	pitch: null,
	whatsappLink: "https://wa.me/244923456789",
	websiteCrawl: null,
};

const EXCLUDED: BackendLead = { ...QUALIFIED, id: 9002, name: "Mock Banco", stage: "EXCLUDED", score: 99 };

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
});

describe("backend lead mapping", () => {
	it("keeps scores and reasons as data", () => {
		const lead = toLead(QUALIFIED, 1);
		expect(lead).toMatchObject({
			id: "9001",
			rank: 1,
			name: "Mock Sorriso",
			phone: "+244923456789",
			website: null,
			score: 65,
			breakdown: { stage1: [{ points: 5, reason: "Telefone móvel" }], stage2: null },
		});
		expect(lead.pitch).toBe("");
		expect(lead.audit).toEqual([
			{ check: "Website", result: "FAIL", detail: "Não existe. O Google Maps não tem ligação para nenhum site." },
		]);
		expect(lead.complaints).toBeNull();
	});

	it("shows the stage 2 items, the crawl and the complaint kinds once the lead is enriched", () => {
		const lead = toLead(
			{
				...QUALIFIED,
				website: "http://velho.ao",
				websiteKind: "OWN",
				complaintKinds: ["contact", "waiting"],
				stage2Breakdown: [{ code: "NO_HTTPS", points: 25, reason: "Website is plain HTTP, not HTTPS" }],
				websiteCrawl: {
					url: "http://velho.ao",
					reachable: true,
					https: false,
					mobileFriendly: true,
					stale: null,
					httpStatus: 200,
					error: null,
					crawledAt: "2026-10-05T10:00:00Z",
				},
			},
			1,
		);
		expect(lead.breakdown.stage2).toEqual([{ points: 25, reason: "Website is plain HTTP, not HTTPS" }]);
		expect(lead.audit.map((a) => [a.check, a.result])).toEqual([
			["Carrega", "OK"],
			["HTTPS", "FAIL"],
			["Telemóvel", "OK"],
		]);
		expect(lead.complaints).toEqual([
			{ theme: "Difícil de contactar", mentions: null, quotes: [] },
			{ theme: "Demora no atendimento", mentions: null, quotes: [] },
		]);
	});

	it("marks a site with no crawl as not analysed and an unreachable one as failed", () => {
		const own = { ...QUALIFIED, website: "https://nova.ao", websiteKind: "OWN" as const };
		expect(toLead(own, 1).audit).toMatchObject([{ check: "Website", result: "PENDING" }]);
		const down = {
			url: "https://nova.ao",
			reachable: false,
			https: null,
			mobileFriendly: null,
			stale: null,
			httpStatus: null,
			error: "Website unreachable",
			crawledAt: "2026-10-05T10:00:00Z",
		};
		expect(toLead({ ...own, websiteCrawl: down }, 1).audit).toEqual([
			{ check: "Carrega", result: "FAIL", detail: "Website unreachable" },
		]);
	});

	it("splits the host and scheme off website URLs", () => {
		expect(toLead({ ...QUALIFIED, website: "https://nova.ao" }, 1).website).toEqual({
			host: "nova.ao",
			https: true,
		});
	});

	it("maps the outcome fields from the API", () => {
		const lead = toLead({ ...QUALIFIED, status: "LOST", lostReason: "NOT_NOW", note: "falar em marco" }, null);
		expect(lead).toMatchObject({ status: "LOST", lostReason: "NOT_NOW", note: "falar em marco" });
	});
});

describe("leads over HTTP", () => {
	it("reads every lead with the rank the API gives it", async () => {
		const fetch = stubApi({
			"/leads?limit=2000": [
				{ ...QUALIFIED, rank: 1 },
				{ ...EXCLUDED, rank: null },
			],
		});
		const leads = await fetchLeads();
		expect(fetch).toHaveBeenCalledTimes(1);
		expect(leads.map((l) => [l.name, l.rank])).toEqual([
			["Mock Sorriso", 1],
			["Mock Banco", null],
		]);
	});

	it("reads one lead without a list-scoped rank", async () => {
		stubApi({ "/leads/9001": QUALIFIED });
		await expect(fetchLead("9001")).resolves.toMatchObject({ id: "9001", name: "Mock Sorriso", rank: null });
	});

	it("reads the today queue from the API without cutting it", async () => {
		const fetch = stubApi({ "/leads/today": [QUALIFIED] });
		const queue = await fetchTodayQueue();
		expect(fetch).toHaveBeenCalledWith("http://api:8080/api/leads/today", expect.anything());
		expect(queue.map((l) => l.name)).toEqual(["Mock Sorriso"]);
		expect(queue[0].rank).toBe(1);
	});
});

describe("markLead", () => {
	it("saves the outcome on the fixtures without an API", async () => {
		const before = (await fetchLeads()).find((l) => l.id === "l1");
		try {
			const lead = await markLead("l1", { status: "CONTACTED", note: "ligou" });
			expect(lead).toMatchObject({ status: "CONTACTED", note: "ligou" });
		} finally {
			await markLead("l1", {
				status: before?.status ?? "NEW",
				lostReason: before?.lostReason ?? null,
				note: before?.note ?? null,
			});
		}
		await expect(fetchLead("l1")).resolves.toMatchObject({
			status: before?.status,
			note: before?.note ?? null,
		});
	});

	it("PATCHes the lead over HTTP", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://api:8080/api");
		let method: string | undefined;
		let body: unknown;
		vi.stubGlobal(
			"fetch",
			vi.fn(async (_url: string, init?: { method?: string; body?: string }) => {
				method = init?.method;
				body = JSON.parse(init?.body ?? "{}");
				return new Response(JSON.stringify({ ...QUALIFIED, status: "CONTACTED", lostReason: null, note: "ligou" }), {
					status: 200,
				});
			}),
		);
		const lead = await markLead("9001", { status: "CONTACTED", note: "ligou" });
		expect(method).toBe("PATCH");
		expect(body).toEqual({ status: "CONTACTED", lostReason: null, note: "ligou" });
		expect(lead).toMatchObject({ id: "9001", status: "CONTACTED", note: "ligou" });
	});
});

describe("pitch", () => {
	it("shows the backend pitch and an empty string while there is none", () => {
		expect(toLead({ ...QUALIFIED, pitch: "Bom dia, podemos falar?" }, 1).pitch).toBe("Bom dia, podemos falar?");
		expect(toLead(QUALIFIED, 1).pitch).toBe("");
	});

	it("POSTs to the pitch endpoint and returns the new pitch", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://api:8080/api");
		let url: string | undefined;
		let method: string | undefined;
		vi.stubGlobal(
			"fetch",
			vi.fn(async (u: string, init?: { method?: string }) => {
				url = u;
				method = init?.method;
				return new Response(JSON.stringify({ ...QUALIFIED, pitch: "Nova mensagem" }), { status: 200 });
			}),
		);
		const lead = await regeneratePitch("9001");
		expect(url).toBe("http://api:8080/api/leads/9001/pitch");
		expect(method).toBe("POST");
		expect(lead.pitch).toBe("Nova mensagem");
	});
});
