import { queryOptions } from "@tanstack/react-query";
import { fetchUsage } from "./api";

/**
 * One month of spend, optionally for one campaign. Without a month it is this month, which only the server
 * knows (the fixtures have their own), so pages read `month` from the result.
 */
export const usageQuery = (month?: string, campaign?: string) =>
	queryOptions({
		queryKey: ["usage", month ?? "current", campaign ?? null],
		queryFn: () => fetchUsage({ data: { month, campaign } }),
	});
