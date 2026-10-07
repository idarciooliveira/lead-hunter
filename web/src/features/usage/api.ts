import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { orNotFound } from "#/lib/server-fn";
import * as source from "./api.server";

/**
 * Server function (ADR 0037): one month of spend, this month when `month` is missing, every campaign when `campaign` is.
 * A campaign the API does not know (deleted after a bookmark) shows the router's not-found page.
 */
export const fetchUsage = createServerFn({ method: "GET" })
	.inputValidator(
		z.object({
			month: z
				.string()
				.regex(/^\d{4}-\d{2}$/)
				.optional(),
			campaign: z.string().min(1).optional(),
		}),
	)
	.handler(({ data }) => orNotFound(() => source.fetchUsage(data.month, data.campaign)));
