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
	const base = apiBaseUrl();
	if (!base) throw new Error(`no API configured: set VITE_LEADHUNTER_API_URL to fetch ${path}`);
	const res = await fetch(`${base}${path}`);
	if (!res.ok) {
		const message = await errorMessage(res, path);
		if (res.status === 404) throw new NotFoundError(message);
		throw new Error(message);
	}
	return schema.parse(await res.json());
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
