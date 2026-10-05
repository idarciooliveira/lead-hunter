import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import * as source from "./api.server";

/** Server function (ADR 0037): one month of spend, this month when `month` is missing. */
export const fetchUsage = createServerFn({ method: "GET" })
	.inputValidator(
		z
			.string()
			.regex(/^\d{4}-\d{2}$/)
			.optional(),
	)
	.handler(({ data: month }) => source.fetchUsage(month));
