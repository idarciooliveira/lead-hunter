import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { orNotFound } from "#/lib/server-fn";
import * as source from "./api.server";

/** Server functions (ADR 0037): the browser calls these, only the server calls the API. */
export const fetchCampaignRuns = createServerFn({ method: "GET" })
	.inputValidator(z.string())
	.handler(({ data: slug }) => orNotFound(() => source.fetchCampaignRuns(slug)));

export const previewScrape = createServerFn({ method: "POST" })
	.inputValidator(z.string())
	.handler(({ data: slug }) => source.previewScrape(slug));

export const startScrape = createServerFn({ method: "POST" })
	.inputValidator(z.object({ slug: z.string(), allowOverLimit: z.boolean() }))
	.handler(({ data }) => source.startScrape(data.slug, data.allowOverLimit));

export const previewEnrichment = createServerFn({ method: "POST" })
	.inputValidator(z.string())
	.handler(({ data: slug }) => source.previewEnrichment(slug));

export const startEnrichment = createServerFn({ method: "POST" })
	.inputValidator(z.string())
	.handler(({ data: slug }) => source.startEnrichment(slug));
