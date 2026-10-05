/**
 * Where the backend JSON API lives (docs/api.md). Set `VITE_LEADHUNTER_API_URL`
 * to the API root, e.g. `http://localhost:8080/api`, and the feature `api.ts`
 * files fetch real responses. Unset, every page keeps reading its fixtures.
 */
export function apiBaseUrl(): string | null {
	const raw = import.meta.env.VITE_LEADHUNTER_API_URL as string | undefined;
	if (!raw || !raw.trim()) return null;
	return raw.trim().replace(/\/+$/, "");
}
