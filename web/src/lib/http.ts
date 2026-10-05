import type { z } from "zod";
import { apiBaseUrl } from "./api-config";
import { NotFoundError } from "./fake-api";

/**
 * GETs `path` from the backend API and validates the response with the same
 * Zod schema the fixtures go through. 404 becomes `NotFoundError`, so routes
 * keep showing their not-found page; anything else throws an `Error` with the
 * backend's message (`docs/api.md` mirrors the CLI's `error: <message>`).
 */
export async function apiFetch<T extends z.ZodType>(schema: T, path: string): Promise<z.infer<T>> {
	const res = await request(path);
	if (!res.ok) throw await toError(res, path);
	return schema.parse(await res.json());
}

/**
 * Sends a JSON body (`PATCH` today) and validates the response like `apiFetch`.
 * Backend-shaped errors reach the UI verbatim, never as a generic failure.
 */
export async function apiMutate<T extends z.ZodType>(
	schema: T,
	path: string,
	method: "PATCH",
	body: unknown,
): Promise<z.infer<T>> {
	const res = await request(path, method, body);
	if (!res.ok) throw await toError(res, path);
	return schema.parse(await res.json());
}

async function request(path: string, method?: "PATCH", body?: unknown): Promise<Response> {
	const base = apiBaseUrl();
	if (!base) throw new Error(`no API configured: set VITE_LEADHUNTER_API_URL to fetch ${path}`);
	return fetch(
		`${base}${path}`,
		method === undefined
			? undefined
			: { method, headers: { "content-type": "application/json" }, body: JSON.stringify(body) },
	);
}

async function toError(res: Response, path: string): Promise<Error> {
	const message = await errorMessage(res, path);
	if (res.status === 404) return new NotFoundError(message);
	return new Error(message);
}

async function errorMessage(res: Response, path: string): Promise<string> {
	try {
		const body = (await res.json()) as { message?: unknown };
		if (typeof body.message === "string" && body.message) return body.message;
	} catch {
		// Fall through to the status line below.
	}
	return `${path} failed with status ${res.status}`;
}
