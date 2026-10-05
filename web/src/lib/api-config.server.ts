/**
 * Where the backend JSON API lives (docs/api.md) and the service token it
 * expects (ADR 0037). Both are server-only: set `LEADHUNTER_API_URL` to the
 * API root, e.g. `http://localhost:8080/api`, and the server functions fetch
 * real responses. Unset, every page keeps reading its fixtures.
 */
export function apiBaseUrl(): string | null {
	const raw = process.env.LEADHUNTER_API_URL;
	if (!raw || !raw.trim()) return null;
	return raw.trim().replace(/\/+$/, "");
}

/** Sent as a bearer token when set. The backend starts requiring it with ADR 0037. */
export function apiToken(): string | null {
	const raw = process.env.LEADHUNTER_API_TOKEN;
	return raw?.trim() ? raw.trim() : null;
}
