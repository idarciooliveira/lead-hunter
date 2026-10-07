import { apiBaseUrl } from "#/lib/api-config.server";
import { BackendCompanyProfile, BackendSaveResult } from "#/lib/api-contract";
import { fakeResponse, NotFoundError } from "#/lib/fake-api";
import { apiFetch, apiMutate } from "#/lib/http.server";
import { COMPANY } from "./fixtures";
import { CompanyProfile } from "./schema";

/** GET /api/company. Null until `company setup` has saved a profile (the API answers 404). */
export async function fetchCompany(): Promise<CompanyProfile | null> {
	if (apiBaseUrl() === null) return fakeResponse(CompanyProfile, COMPANY);
	try {
		// The API serves the profile in the UI's shape, so it parses straight through.
		return await apiFetch(CompanyProfile, "/company");
	} catch (e) {
		if (e instanceof NotFoundError) return null;
		throw e;
	}
}

/** What a write answers: the saved profile and the CLI's non-blocking warnings. */
export type SaveCompany = { saved: CompanyProfile; warnings: string[] };

/**
 * Saves the company profile: PUT /api/company (docs/api.md Writes). The UI
 * only edits part of the profile, so over HTTP the input is merged over the
 * stored one and the CLI-only fields (objections, capacity, quarter target) survive.
 * Without an API configured the profile lands on the in-memory fixtures for
 * the session, so the pages keep working in the smoke build.
 */
export async function saveCompany(input: CompanyProfile): Promise<SaveCompany> {
	if (apiBaseUrl() === null) {
		Object.assign(COMPANY, input);
		const saved = await fakeResponse(CompanyProfile, COMPANY);
		return { saved, warnings: fixtureWarnings(saved) };
	}
	const current = await apiFetch(BackendCompanyProfile, "/company").catch((e) => {
		// No profile yet: the PUT creates it, so only the UI fields go over.
		if (e instanceof NotFoundError) return null;
		throw e;
	});
	const body = current === null ? input : { ...current, ...input };
	const res = await apiMutate(BackendSaveResult(CompanyProfile), "/company", "PUT", body);
	return { saved: await fakeResponse(CompanyProfile, res.saved), warnings: res.warnings };
}

/**
 * The backend's CompanyProfileParser.warnings rule, for the fixtures without
 * an API: clients without a phone are matched by name only.
 */
function fixtureWarnings(profile: CompanyProfile): string[] {
	const withoutPhone = profile.clients.filter((c) => !c.phone?.trim()).map((c) => c.name);
	if (withoutPhone.length === 0) return [];
	return [
		`clients without a valid phone are matched by name only, which misses name variants on Google Maps: ${withoutPhone.join(", ")}`,
	];
}
