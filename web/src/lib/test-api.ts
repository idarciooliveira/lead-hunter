import { vi } from "vitest";
import type { BackendCampaign } from "./api-contract";

/**
 * Points the feature `api.ts` files at a fake backend for one test: each path
 * answers its JSON with 200, any other path 404 with a backend-shaped message.
 * Pair it with `vi.unstubAllEnvs()` and `vi.unstubAllGlobals()` in `afterEach`.
 */
export function stubApi(routes: Record<string, unknown>) {
	vi.stubEnv("LEADHUNTER_API_URL", "http://api:8080/api");
	const fetch = vi.fn(async (url: string, _init?: RequestInit) => {
		const path = url.replace("http://api:8080/api", "");
		if (!(path in routes)) return new Response(JSON.stringify({ message: `no route ${path}` }), { status: 404 });
		return new Response(JSON.stringify(routes[path]), { status: 200 });
	});
	vi.stubGlobal("fetch", fetch);
	return fetch;
}

/** A campaign as GET /api/campaigns serves it, never run. */
export const BACKEND_CAMPAIGN: BackendCampaign = {
	slug: "mock-clinicas",
	name: "Mock Clínicas",
	answers: { sector: "Clínicas", service: "Site" },
	search: { locations: ["Talatona"] },
	totalCostUsd: 0.21,
	qualifiedCount: 1,
	latestRun: null,
};
