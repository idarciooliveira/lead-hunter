import { queryOptions, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchLead, fetchLeads, fetchTodayQueue, type MarkLeadInput, markLead, regeneratePitch } from "./api";

// Every key starts with "leads", so invalidating ["leads"] refreshes all of them.
export const leadsQuery = () => queryOptions({ queryKey: ["leads", "list"], queryFn: () => fetchLeads() });

export const leadQuery = (id: string) =>
	queryOptions({ queryKey: ["leads", "detail", id], queryFn: () => fetchLead({ data: id }) });

export const todayQueueQuery = () => queryOptions({ queryKey: ["leads", "today"], queryFn: () => fetchTodayQueue() });

/** Marks a contact outcome, then refreshes every leads query. Backend errors reach the UI verbatim. */
export const useMarkLead = () => {
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: ({ id, input }: { id: string; input: MarkLeadInput }) => markLead({ data: { id, input } }),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: ["leads"] }),
	});
};

/** Writes a new pitch for a lead, then refreshes every leads query. */
export const useRegeneratePitch = () => {
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: (id: string) => regeneratePitch({ data: id }),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: ["leads"] }),
	});
};
