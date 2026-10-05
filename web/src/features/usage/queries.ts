import { queryOptions } from "@tanstack/react-query";
import { currentUsageMonth, fetchUsage } from "./api";

export { currentUsageMonth };

/** One month of spend; this month by default. */
export const usageQuery = (month: string = currentUsageMonth()) =>
	queryOptions({ queryKey: ["usage", month], queryFn: () => fetchUsage(month) });
