import { afterEach, describe, expect, it, vi } from "vitest";
import { z } from "zod";
import { NotFoundError } from "./fake-api";
import { apiFetch } from "./http.server";

const Schema = z.object({ slug: z.string() });

function response(status: number, body: unknown): Response {
	return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
});

describe("apiFetch", () => {
	it("needs LEADHUNTER_API_URL", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "");
		await expect(apiFetch(Schema, "/campaigns")).rejects.toThrow("LEADHUNTER_API_URL");
	});

	it("fetches and validates the response", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://api:8080/api");
		const fetch = vi.fn(async (url: string) => {
			expect(url).toBe("http://api:8080/api/campaigns");
			return response(200, [{ slug: "x", extra: 1 }]);
		});
		vi.stubGlobal("fetch", fetch);
		await expect(apiFetch(z.array(Schema), "/campaigns")).resolves.toEqual([{ slug: "x" }]);
	});

	it("turns 404 into NotFoundError with the backend message", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://api:8080/api");
		vi.stubGlobal(
			"fetch",
			vi.fn(async () => response(404, { message: "no campaign 'x'" })),
		);
		await expect(apiFetch(Schema, "/campaigns/x")).rejects.toMatchObject({
			name: "Error",
			message: "no campaign 'x'",
		});
		await expect(apiFetch(Schema, "/campaigns/x").catch((e) => e)).resolves.toBeInstanceOf(NotFoundError);
	});

	it("throws other failures with the backend message", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://api:8080/api");
		vi.stubGlobal(
			"fetch",
			vi.fn(async () => response(400, { message: "unknown stage 'BOGUS'" })),
		);
		await expect(apiFetch(Schema, "/campaigns")).rejects.toThrow("unknown stage 'BOGUS'");
	});

	it("sends the service token when one is set (ADR 0037)", async () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://api:8080/api");
		vi.stubEnv("LEADHUNTER_API_TOKEN", "secret");
		const fetch = vi.fn(async (_url: string, _init?: RequestInit) => response(200, { slug: "x" }));
		vi.stubGlobal("fetch", fetch);
		await apiFetch(Schema, "/campaigns/x");
		expect(fetch.mock.calls[0][1]?.headers).toEqual({ authorization: "Bearer secret" });
	});
});
