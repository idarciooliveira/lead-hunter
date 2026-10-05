import { fakeResponse } from "#/lib/fake-api";
import { COMPANY } from "./fixtures";
import { CompanyProfile } from "./schema";

/** Becomes GET /api/company. */
export async function fetchCompany(): Promise<CompanyProfile> {
	return fakeResponse(CompanyProfile, COMPANY);
}
