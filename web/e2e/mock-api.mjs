// Minimal stand-in for the backend API (docs/api.md), so the Playwright
// `integration` project proves the web app renders real HTTP responses.
// The MockMvc suite proves the real backend serves this shape. PATCH mirrors
// LeadRepository.updateOutcome; POST /__reset restores the fixtures and only
// exists for test isolation.
import http from "node:http";

const PORT = Number(process.env.MOCK_API_PORT ?? 3330);

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
		whatsappLink: null,
	},
];

const PRISTINE = structuredClone(LEADS);

const server = http.createServer((req, res) => {
	const url = new URL(req.url ?? "/", "http://localhost");
	const json = (status, body) => {
		res.writeHead(status, {
			"content-type": "application/json",
			// The mutations run in the browser from another origin, like against the real API (ApiCorsConfig).
			"access-control-allow-origin": "*",
			"access-control-allow-methods": "GET, PATCH, POST, OPTIONS",
			"access-control-allow-headers": "*",
		});
		res.end(JSON.stringify(body));
	};
	if (req.method === "OPTIONS") {
		res.writeHead(204, {
			"access-control-allow-origin": "*",
			"access-control-allow-methods": "GET, PATCH, POST, OPTIONS",
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
		return json(200, { status: "ok" });
	}
	if (req.method === "GET" && url.pathname === "/api/health") return json(200, { status: "ok" });
	if (req.method === "GET" && url.pathname === "/api/campaigns") {
		return json(200, [{ slug: "mock-clinicas", name: "Mock Clínicas" }]);
	}
	if (req.method === "GET" && url.pathname === "/api/campaigns/mock-clinicas/leads") {
		const stage = url.searchParams.get("stage") ?? "QUALIFIED";
		return json(200, stage === "ALL" ? LEADS : LEADS.filter((l) => l.stage === stage));
	}
	const campaignLeadsMatch = /^\/api\/campaigns\/([^/]+)\/leads$/.exec(url.pathname);
	if (req.method === "GET" && campaignLeadsMatch) {
		return json(404, { message: `no campaign '${campaignLeadsMatch[1]}'. Run: campaign list` });
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

server.listen(PORT, () => console.log(`mock api on :${PORT}`));
