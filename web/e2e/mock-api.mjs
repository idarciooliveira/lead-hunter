// Minimal stand-in for the backend API (docs/api.md), so the Playwright
// `integration` project proves the web app renders real HTTP responses.
// The MockMvc suite proves the real backend serves this shape. PATCH mirrors
// LeadRepository.updateOutcome, the campaign and company writes mirror their
// controllers' status codes and messages; POST /__reset restores the fixtures
// and only exists for test isolation.
import http from "node:http";

const PORT = Number(process.env.MOCK_API_PORT ?? 3330);
// When set, every /api call but health needs it, like the backend under ADR 0037.
const TOKEN = process.env.MOCK_API_TOKEN;
// When set, every /api call but health must also name this user and organization, like the backend under ADR 0043.
const USER = process.env.MOCK_API_USER;
const ORG = process.env.MOCK_API_ORG;

const STATUSES = ["NEW", "CONTACTED", "NO_ANSWER", "INTERESTED", "MEETING", "PROPOSAL_SENT", "WON", "LOST"];
const LOST_REASONS = ["NO_BUDGET", "WRONG_PERSON", "HAS_SUPPLIER", "NOT_INTERESTED", "NOT_NOW"];

const LEADS = [
	{
		id: 9001,
		campaignSlug: "mock-clinicas",
		stage: "QUALIFIED",
		status: "NEW",
		lostReason: null,
		note: null,
		score: 65,
		breakdown: [{ code: "MOBILE_PHONE", points: 5, reason: "Telefone móvel" }],
		stageReason: null,
		name: "Mock Sorriso",
		category: "Clínica",
		address: "Rua 1, Talatona",
		neighborhood: "Talatona",
		phoneE164: "+244923456789",
		phoneMobile: true,
		website: null,
		websiteKind: "NONE",
		rating: 4.3,
		reviewsCount: 142,
		mapsUrl: "https://maps.example/p1",
		complaintKinds: ["contact"],
		pitch: "Bom dia, notámos que ninguém atende o telefone. Podemos falar?",
		whatsappLink: "https://wa.me/244923456789",
	},
	{
		id: 9002,
		campaignSlug: "mock-clinicas",
		stage: "BELOW_CUT",
		status: "NEW",
		lostReason: null,
		note: null,
		score: 20,
		breakdown: [],
		stageReason: "Ranked 2 of 2, below the top 40% cut",
		name: "Mock Girassol",
		category: "Clínica",
		address: "Rua 2, Maianga",
		neighborhood: "Maianga",
		phoneE164: "+244923000111",
		phoneMobile: false,
		website: "https://girassol.example",
		websiteKind: "OWN",
		rating: 4.0,
		reviewsCount: 50,
		mapsUrl: "https://maps.example/p2",
		complaintKinds: [],
		pitch: null,
		whatsappLink: null,
	},
];

const CAMPAIGN = {
	id: 1,
	slug: "mock-clinicas",
	name: "Mock Clínicas",
	answers: { sector: "Mock clínicas privadas", service: "Mock sistema de marcações" },
	search: { terms: ["clínica"], locations: ["Talatona", "Kilamba"] },
	createdAt: "2026-10-01T09:00:00Z",
	totalCostUsd: 0.21,
	qualifiedCount: 1,
	latestRun: null,
	funnel: {
		scraped: 12,
		scrapeCostUsd: 0.2,
		kept: 9,
		excluded: 3,
		excludedBy: [{ reason: "Sem telefone", leads: 3 }],
		cutShare: 0.4,
		qualified: 1,
		enriched: 1,
		enrichCostUsd: 0.01,
		enriching: false,
	},
};

const RUNS = [];

// Created campaigns join this list, so the pages read them back like from the real API.
const CAMPAIGNS = [CAMPAIGN];

const COMPANY = {
	name: "Mock Software, Lda.",
	intro: null,
	services: [{ name: "Mock marcações online", price: "1 000 000 Kz", deliveryTime: "6 semanas" }],
	entryOffer: null,
	area: ["Talatona"],
	clients: [{ name: "Mock Cliente Antigo", phone: null }],
	cases: [],
	objections: [],
	weeklyCapacity: 35,
	quarterTarget: null,
};

const USAGE = {
	scope: "month",
	apify: { runs: 1, failedRuns: 0, unpricedRuns: 0, places: 8, costUsd: 0.2 },
	llm: { calls: 1, unpricedCalls: 0, promptTokens: 100, completionTokens: 50, costUsd: 0.01, models: [] },
	byCampaign: [{ slug: "mock-clinicas", apifyUsd: 0.2, llmUsd: 0.01 }],
	totalUsd: 0.21,
	budgetUsd: 10,
	committedUsd: 0.21,
};

const ENTRIES = [
	{
		kind: "apify",
		at: "2026-10-05T09:41:00Z",
		campaignSlug: "mock-clinicas",
		label: "Mock Talatona, SUCCEEDED",
		costUsd: 0.2,
	},
];

const PRISTINE = structuredClone(LEADS);
const PRISTINE_CAMPAIGNS = structuredClone(CAMPAIGNS);
const PRISTINE_COMPANY = structuredClone(COMPANY);

const server = http.createServer((req, res) => {
	const url = new URL(req.url ?? "/", "http://localhost");
	const json = (status, body) => {
		res.writeHead(status, {
			"content-type": "application/json",
			// The mutations run in the browser from another origin, like against the real API (ApiCorsConfig).
			"access-control-allow-origin": "*",
			"access-control-allow-methods": "GET, PATCH, POST, PUT, OPTIONS",
			"access-control-allow-headers": "*",
		});
		res.end(JSON.stringify(body));
	};
	if (req.method === "OPTIONS") {
		res.writeHead(204, {
			"access-control-allow-origin": "*",
			"access-control-allow-methods": "GET, PATCH, POST, PUT, OPTIONS",
			"access-control-allow-headers": "*",
		});
		return res.end();
	}
	const readBody = (done) => {
		let raw = "";
		req.on("data", (chunk) => (raw += chunk));
		req.on("end", () => {
			try {
				done(JSON.parse(raw || "{}"));
			} catch {
				json(400, { message: "invalid JSON body" });
			}
		});
	};
	if (req.method === "POST" && url.pathname === "/__reset") {
		LEADS.length = 0;
		for (const lead of structuredClone(PRISTINE)) LEADS.push(lead);
		RUNS.length = 0;
		CAMPAIGNS.length = 0;
		for (const campaign of structuredClone(PRISTINE_CAMPAIGNS)) CAMPAIGNS.push(campaign);
		const fresh = structuredClone(PRISTINE_COMPANY);
		for (const key of Object.keys(COMPANY)) delete COMPANY[key];
		Object.assign(COMPANY, fresh);
		return json(200, { status: "ok" });
	}
	if (req.method === "GET" && url.pathname === "/api/health") return json(200, { status: "ok" });
	if (TOKEN && url.pathname.startsWith("/api/") && req.headers.authorization !== `Bearer ${TOKEN}`) {
		return json(401, { message: "missing or wrong API token" });
	}
	if (
		url.pathname.startsWith("/api/") &&
		((USER && req.headers["x-leadhunter-user"] !== USER) || (ORG && req.headers["x-leadhunter-org"] !== ORG))
	) {
		return json(403, { message: "missing or wrong user or organization header" });
	}
	if (req.method === "POST" && url.pathname === "/api/campaigns/mock-clinicas/enrichment") {
		return json(400, { message: "nothing to enrich: every qualified lead of mock-clinicas is enriched" });
	}
	if (req.method === "GET" && url.pathname === "/api/campaigns") return json(200, CAMPAIGNS);
	if (req.method === "POST" && url.pathname === "/api/campaigns") {
		return readBody((body) => {
			// Mirrors CampaignController + CampaignFileParser + CampaignChecks.requireFits.
			const problems = campaignProblems(body);
			if (problems.length > 0) return json(400, invalidBody("campaign file", problems));
			if (CAMPAIGNS.some((c) => c.slug === body.slug)) {
				return json(409, { message: `campaign '${body.slug}' already exists. Update it instead` });
			}
			if (!COMPANY.services.some((s) => s.name === body.answers.service)) {
				const names = COMPANY.services.map((s) => s.name).join(", ");
				return json(
					400,
					invalidBody("campaign file", [
						`answers.service '${body.answers.service}' is not one of the company's services: ${names}`,
					]),
				);
			}
			const saved = {
				slug: body.slug,
				name: body.name,
				answers: { sector: body.answers.sector, service: body.answers.service },
				search: { terms: body.search.terms, locations: body.search.locations },
				totalCostUsd: 0,
				qualifiedCount: 0,
				latestRun: null,
				funnel: null,
			};
			CAMPAIGNS.push(saved);
			return json(201, { saved, warnings: [] });
		});
	}
	const campaignRunsMatch = /^\/api\/campaigns\/([^/]+)\/runs$/.exec(url.pathname);
	if (req.method === "GET" && campaignRunsMatch) {
		if (!CAMPAIGNS.some((c) => c.slug === campaignRunsMatch[1])) {
			return json(404, { message: `no campaign '${campaignRunsMatch[1]}'. Run: campaign list` });
		}
		return json(
			200,
			RUNS.filter((r) => r.campaignSlug === campaignRunsMatch[1]),
		);
	}
	if (req.method === "POST" && url.pathname === "/api/campaigns/mock-clinicas/runs") {
		// Mirrors RunController: the dry run is free and over the limit here, so a start needs the opt-in.
		if (url.searchParams.get("dryRun") === "true") {
			return json(200, {
				requests: [
					{ location: "Talatona", terms: ["clínica"], maxPlaces: 120 },
					{ location: "Kilamba", terms: ["clínica"], maxPlaces: 120 },
				],
				maxPlaces: 240,
				estimatedMaxUsd: 0.96,
				overLimit: true,
				dryRunId: 1,
			});
		}
		if (url.searchParams.get("allowOverLimit") !== "true") {
			return json(400, { message: "this run could return up to 240 places, above the limit of 150" });
		}
		if (RUNS.some((r) => r.status === "RUNNING")) {
			return json(409, { message: "campaign mock-clinicas already has a run in progress" });
		}
		// Finishes at once, so the page polls one round and settles.
		const run = {
			id: 100 + RUNS.length,
			campaignSlug: "mock-clinicas",
			kind: "SCRAPE",
			status: "DONE",
			startedAt: new Date().toISOString(),
			done: 8,
			total: null,
			costUsd: 0.2,
			error: null,
		};
		RUNS.unshift(run);
		const campaign = CAMPAIGNS.find((c) => c.slug === run.campaignSlug);
		if (campaign) campaign.latestRun = run;
		return json(202, run);
	}
	if (req.method === "GET" && url.pathname === "/api/company") return json(200, COMPANY);
	if (req.method === "PUT" && url.pathname === "/api/company") {
		return readBody((body) => {
			// Mirrors CompanyController: the UI shape merges over the stored
			// profile, keeping the CLI-only fields the pages never edit.
			// Validate the candidate first, so a 400 leaves stored state alone.
			const candidate = { ...COMPANY, ...body };
			const problems = [];
			if (!candidate.name?.trim()) problems.push("name is required");
			if (!candidate.services?.length) problems.push("services needs at least one service");
			if (problems.length > 0) return json(400, invalidBody("company profile", problems));
			Object.assign(COMPANY, body);
			return json(200, { saved: COMPANY, warnings: companyWarnings() });
		});
	}
	if (req.method === "GET" && url.pathname === "/api/usage") return json(200, USAGE);
	if (req.method === "GET" && url.pathname === "/api/usage/entries") return json(200, ENTRIES);
	if (req.method === "GET" && url.pathname === "/api/leads/today") {
		// Mirrors LeadRepository.today: QUALIFIED still NEW, best first.
		const queue = LEADS.filter((l) => l.stage === "QUALIFIED" && l.status === "NEW").sort(
			(a, b) => b.score - a.score || b.reviewsCount - a.reviewsCount || a.id - b.id,
		);
		return json(200, queue);
	}
	if (req.method === "GET" && url.pathname === "/api/campaigns/mock-clinicas/leads") {
		const stage = url.searchParams.get("stage") ?? "QUALIFIED";
		return json(200, stage === "ALL" ? LEADS : LEADS.filter((l) => l.stage === stage));
	}
	const campaignLeadsMatch = /^\/api\/campaigns\/([^/]+)\/leads$/.exec(url.pathname);
	if (req.method === "GET" && campaignLeadsMatch) {
		return json(404, { message: `no campaign '${campaignLeadsMatch[1]}'. Run: campaign list` });
	}
	const campaignMatch = /^\/api\/campaigns\/([^/]+)$/.exec(url.pathname);
	if (req.method === "GET" && campaignMatch) {
		const found = CAMPAIGNS.find((c) => c.slug === campaignMatch[1]);
		if (!found) return json(404, { message: `no campaign '${campaignMatch[1]}'. Run: campaign list` });
		return json(200, found);
	}
	const leadMatch = /^\/api\/leads\/(\d+)$/.exec(url.pathname);
	if (leadMatch) {
		const lead = LEADS.find((l) => l.id === Number(leadMatch[1]));
		if (req.method === "GET") {
			if (!lead) return json(404, { message: `no lead with id ${leadMatch[1]}` });
			return json(200, lead);
		}
		if (req.method === "PATCH") {
			if (!lead) return json(404, { message: `no lead with id ${leadMatch[1]}` });
			return readBody((body) => {
				const status = typeof body.status === "string" ? body.status.toUpperCase() : null;
				const lostReason =
					body.lostReason === null || body.lostReason === undefined ? null : String(body.lostReason).toUpperCase();
				// Mirrors LeadController + LeadRepository.updateOutcome.
				if (status === null)
					return json(400, {
						message: "status is required: NEW, CONTACTED, NO_ANSWER, INTERESTED, MEETING, PROPOSAL_SENT, WON or LOST",
					});
				if (!STATUSES.includes(status)) return json(400, { message: `unknown status '${body.status}'` });
				if (status === "LOST" && !LOST_REASONS.includes(lostReason)) {
					if (lostReason === null) {
						return json(400, {
							message: `marking lead ${lead.id} LOST needs a lost reason: NO_BUDGET, WRONG_PERSON, HAS_SUPPLIER, NOT_INTERESTED or NOT_NOW`,
						});
					}
					return json(400, { message: `unknown lost reason '${body.lostReason}'` });
				}
				if (status !== "LOST" && lostReason !== null) {
					return json(400, { message: `a lost reason needs status LOST, got ${status}` });
				}
				if (status === "NEW" && lead.status !== "NEW") {
					return json(400, {
						message: `lead ${lead.id} is already ${lead.status}; marking it NEW again would hide the contact history`,
					});
				}
				lead.status = status;
				lead.lostReason = status === "LOST" ? lostReason : null;
				lead.note = typeof body.note === "string" && body.note.trim() !== "" ? body.note : null;
				return json(200, lead);
			});
		}
	}
	return json(404, { message: `no mock for ${url.pathname}` });
});

// Mirrors CampaignFileParser.validate: each broken rule on its own, like InvalidInputException.
const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;
function campaignProblems(body) {
	const problems = [];
	if (!SLUG.test(body.slug ?? ""))
		problems.push("slug must be lowercase letters, digits and dashes, like clinicas-luanda");
	if (!body.name?.trim()) problems.push("name is required");
	const answers = body.answers ?? {};
	if (!answers.sector?.trim()) problems.push("answers.sector is required");
	if (!answers.problem?.trim()) problems.push("answers.problem is required");
	if (!answers.service?.trim()) problems.push("answers.service is required");
	const search = body.search ?? {};
	if (!Array.isArray(search.terms) || search.terms.length === 0) problems.push("search.terms needs at least one term");
	if (!Array.isArray(search.locations) || search.locations.length === 0)
		problems.push("search.locations needs at least one location");
	return problems;
}

function invalidBody(what, problems) {
	return { message: `invalid ${what}:\n  - ${problems.join("\n  - ")}`, problems };
}

// Mirrors CompanyProfileParser.warnings.
function companyWarnings() {
	const withoutPhone = COMPANY.clients.filter((c) => !c.phone?.trim()).map((c) => c.name);
	if (withoutPhone.length === 0) return [];
	return [
		`clients without a valid phone are matched by name only, which misses name variants on Google Maps: ${withoutPhone.join(", ")}`,
	];
}

server.listen(PORT, () => console.log(`mock api on :${PORT}`));
