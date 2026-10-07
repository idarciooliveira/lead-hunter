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

/** Sent as a bearer token when set. The backend refuses every call without it (ADR 0037). */
export function apiToken(): string | null {
	const raw = process.env.LEADHUNTER_API_TOKEN;
	return raw?.trim() ? raw.trim() : null;
}

/** Logins only exist when the API does: without `LEADHUNTER_API_URL` the app shows fixtures and skips them (ADR 0042). */
export function authEnabled(): boolean {
	return apiBaseUrl() !== null;
}

/** What Better Auth needs, read from the environment. Throws one message naming every missing variable. */
export function authSettings(): { databaseUrl: string; secret: string; baseUrl: string } {
	const read = (name: string) => process.env[name]?.trim() ?? "";
	const settings = {
		databaseUrl: read("DATABASE_URL"),
		secret: read("BETTER_AUTH_SECRET"),
		baseUrl: read("BETTER_AUTH_URL"),
	};
	const missing = [
		!settings.databaseUrl && "DATABASE_URL",
		!settings.secret && "BETTER_AUTH_SECRET",
		!settings.baseUrl && "BETTER_AUTH_URL",
	].filter(Boolean);
	if (missing.length > 0) {
		throw new Error(`logins need ${missing.join(", ")}: set them next to LEADHUNTER_API_URL (ADR 0038)`);
	}
	return settings;
}
