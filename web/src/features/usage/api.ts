import { fakeResponse } from "#/lib/fake-api";
import { USAGE } from "./fixtures";
import { type UsageMonth, UsageMonths } from "./schema";

/** Newest month first. Becomes GET /api/usage. */
export async function fetchUsage(): Promise<UsageMonth[]> {
	return fakeResponse(UsageMonths, USAGE);
}
