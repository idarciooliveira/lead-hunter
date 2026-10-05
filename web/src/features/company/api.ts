import { createServerFn } from "@tanstack/react-start";
import * as source from "./api.server";

/** Server function (ADR 0037). Null until `company setup` has saved a profile. */
export const fetchCompany = createServerFn({ method: "GET" }).handler(() => source.fetchCompany());
