import { queryOptions } from "@tanstack/react-query";
import { fetchLead, fetchLeads, fetchTodayQueue } from "./api";

export const leadsQuery = () => queryOptions({ queryKey: ["leads"], queryFn: fetchLeads });

export const leadQuery = (id: string) => queryOptions({ queryKey: ["leads", id], queryFn: () => fetchLead(id) });

export const todayQueueQuery = () => queryOptions({ queryKey: ["leads", "today"], queryFn: fetchTodayQueue });
