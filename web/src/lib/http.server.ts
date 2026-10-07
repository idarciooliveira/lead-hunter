import type { z } from "zod";
import { apiBaseUrl, apiToken } from "./api-config.server";
import { NotFoundError } from "./fake-api";
import { requireViewer } from "./session.server";

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
 * Sends a write (`PATCH`, `POST` or `PUT`), with an optional JSON body, and validates the response like `apiFetch`.
 * Backend-shaped errors reach the UI verbatim, never as a generic failure.
 */
export async function apiMutate<T extends z.ZodType>(
	schema: T,
	path: string,
	method: "PATCH" | "POST" | "PUT",
	body?: unknown,
): Promise<z.infer<T>> {
	const res = await request(path, method, body);
	if (!res.ok) throw await toError(res, path);
	return schema.parse(await res.json());
}

async function request(path: string, method?: "PATCH" | "POST" | "PUT", body?: unknown): Promise<Response> {
	const base = apiBaseUrl();
	if (!base) throw new Error(`no API configured: set LEADHUNTER_API_URL to fetch ${path}`);
	const headers: Record<string, string> = {};
	const token = apiToken();
	if (token) headers.authorization = `Bearer ${token}`;
	// The backend trusts these two headers only because the service token comes with them (ADR 0037, 0043).
	const viewer = await requireViewer();
	headers["x-leadhunter-user"] = viewer.userId;
	headers["x-leadhunter-org"] = viewer.orgId;
	if (body !== undefined) headers["content-type"] = "application/json";
	return fetch(`${base}${path}`, {
		method: method ?? "GET",
		headers,
		body: body === undefined ? undefined : JSON.stringify(body),
	});
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
