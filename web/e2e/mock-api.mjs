// Minimal stand-in for the backend read API (docs/api.md), so the Playwright
// `integration` project proves the web app renders real HTTP responses.
// The MockMvc suite proves the real backend serves this shape.
import http from "node:http";

const PORT = Number(process.env.MOCK_API_PORT ?? 3330);

const LEADS = [
	{
		id: 9001,
		campaignSlug: "mock-clinicas",
		stage: "QUALIFIED",
		status: "NEW",
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

const server = http.createServer((req, res) => {
	const url = new URL(req.url ?? "/", "http://localhost");
	const json = (status, body) => {
		res.writeHead(status, { "content-type": "application/json" });
		res.end(JSON.stringify(body));
	};
	if (req.method === "GET" && url.pathname === "/api/health") return json(200, { status: "ok" });
	if (req.method === "GET" && url.pathname === "/api/campaigns") {
		return json(200, [{ slug: "mock-clinicas", name: "Mock Clínicas" }]);
	}
	if (req.method === "GET" && url.pathname === "/api/campaigns/mock-clinicas/leads") {
		const stage = url.searchParams.get("stage") ?? "QUALIFIED";
		return json(200, stage === "ALL" ? LEADS : LEADS.filter((l) => l.stage === stage));
	}
	const leadMatch = /^\/api\/leads\/(\d+)$/.exec(url.pathname);
	if (req.method === "GET" && leadMatch) {
		const lead = LEADS.find((l) => l.id === Number(leadMatch[1]));
		if (!lead) return json(404, { message: `no lead with id ${leadMatch[1]}` });
		return json(200, lead);
	}
	return json(404, { message: `no mock for ${url.pathname}` });
});

server.listen(PORT, () => console.log(`mock api on :${PORT}`));
