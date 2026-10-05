import { queryOptions, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchCompany, saveCompany } from "./api";
import type { CompanyProfile } from "./schema";

export const companyQuery = () => queryOptions({ queryKey: ["company"], queryFn: () => fetchCompany() });

/** Saves the profile, then refreshes it. Backend errors reach the UI verbatim. */
export const useSaveCompany = () => {
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: (input: CompanyProfile) => saveCompany({ data: input }),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: ["company"] }),
	});
};
