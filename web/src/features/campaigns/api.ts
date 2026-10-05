import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { orNotFound } from "#/lib/server-fn";
import * as source from "./api.server";

/** Server functions (ADR 0037): the browser calls these, only the server calls the API. */
export const fetchCampaigns = createServerFn({ method: "GET" }).handler(() => source.fetchCampaigns());

export const fetchCampaign = createServerFn({ method: "GET" })
	.inputValidator(z.string())
	.handler(({ data: slug }) => orNotFound(() => source.fetchCampaign(slug)));
