import { queryOptions } from "@tanstack/react-query";
import { fetchUsage } from "./api";

export const usageQuery = () => queryOptions({ queryKey: ["usage"], queryFn: fetchUsage });
