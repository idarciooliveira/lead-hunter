import { queryOptions } from "@tanstack/react-query";
import { fetchLead, fetchLeads, fetchTodayQueue } from "./api";

// Every key starts with "leads", so invalidating ["leads"] refreshes all of them.
export const leadsQuery = () => queryOptions({ queryKey: ["leads", "list"], queryFn: fetchLeads });

export const leadQuery = (id: string) =>
	queryOptions({ queryKey: ["leads", "detail", id], queryFn: () => fetchLead(id) });

export const todayQueueQuery = () => queryOptions({ queryKey: ["leads", "today"], queryFn: fetchTodayQueue });
