import { apiBaseUrl } from "#/lib/api-config";
import { fakeResponse, NotFoundError } from "#/lib/fake-api";
import { apiFetch } from "#/lib/http";
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
