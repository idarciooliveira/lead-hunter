import { queryOptions } from "@tanstack/react-query";
import { fetchUsage } from "./api";

/**
 * One month of spend. Without a month it is this month, which only the server
 * knows (the fixtures have their own), so pages read `month` from the result.
 */
export const usageQuery = (month?: string) =>
	queryOptions({ queryKey: ["usage", month ?? "current"], queryFn: () => fetchUsage({ data: month }) });
