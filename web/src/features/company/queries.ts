import { queryOptions } from "@tanstack/react-query";
import { fetchCompany } from "./api";

export const companyQuery = () => queryOptions({ queryKey: ["company"], queryFn: fetchCompany });
