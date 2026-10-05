import { afterEach, describe, expect, it, vi } from "vitest";
import type { BackendLead } from "#/lib/api-contract";
import { fetchLead, fetchLeads, rankBackendLeads, toLead } from "./api";

const QUALIFIED: BackendLead = {
	id: 9001,
	campaignSlug: "mock-clinicas",
	stage: "QUALIFIED",
	status: "NEW",
	score: 65,
	breakdown: [{ code: "MOBILE_PHONE", points: 5, reason: "Telefone móvel" }],
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
	whatsappLink: "https://wa.me/244923456789",
};

const EXCLUDED: BackendLead = { ...QUALIFIED, id: 9002, name: "Mock Banco", stage: "EXCLUDED", score: 99 };

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
});

function stubApi(routes: Record<string, unknown>) {
	vi.stubEnv("VITE_LEADHUNTER_API_URL", "http://api:8080");
	vi.stubGlobal(
		"fetch",
		vi.fn(async (url: string) => {
			const path = url.replace("http://api:8080", "");
			if (!(path in routes)) return new Response("{}", { status: 404 });
			return new Response(JSON.stringify(routes[path]), { status: 200 });
		}),
	);
}

describe("backend lead mapping", () => {
	it("keeps scores and reasons as data and fills what the API does not serve yet", () => {
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
		expect(lead.audit).toEqual([]);
	});

	it("splits the host and scheme off website URLs", () => {
		expect(toLead({ ...QUALIFIED, website: "https://nova.ao" }, 1).website).toEqual({
			host: "nova.ao",
			https: true,
		});
	});

	it("ranks by score with the excluded last and unranked", () => {
		const leads = rankBackendLeads([EXCLUDED, QUALIFIED]);
		expect(leads.map((l) => [l.name, l.rank])).toEqual([
			["Mock Sorriso", 1],
			["Mock Banco", null],
		]);
	});
});

describe("leads over HTTP", () => {
	it("reads every campaign and ranks the leads", async () => {
		stubApi({
			"/campaigns": [{ slug: "mock-clinicas", name: "Mock Clínicas" }],
			"/campaigns/mock-clinicas/leads?stage=ALL&limit=200": [EXCLUDED, QUALIFIED],
		});
		const leads = await fetchLeads();
		expect(leads.map((l) => l.name)).toEqual(["Mock Sorriso", "Mock Banco"]);
	});

	it("reads one lead without a list-scoped rank", async () => {
		stubApi({ "/leads/9001": QUALIFIED });
		await expect(fetchLead("9001")).resolves.toMatchObject({ id: "9001", name: "Mock Sorriso", rank: null });
	});
});
