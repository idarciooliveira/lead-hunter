import { createServerFn } from "@tanstack/react-start";
import * as source from "./api.server";
import { CompanyProfile } from "./schema";

/** Server function (ADR 0037). Null until `company setup` has saved a profile. */
export const fetchCompany = createServerFn({ method: "GET" }).handler(() => source.fetchCompany());

/** PUT /api/company. Backend rule errors keep their message. */
export const saveCompany = createServerFn({ method: "POST" })
	.inputValidator(CompanyProfile)
	.handler(({ data }) => source.saveCompany(data));
