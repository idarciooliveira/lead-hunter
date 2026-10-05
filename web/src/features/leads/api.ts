import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { orNotFound } from "#/lib/server-fn";
import * as source from "./api.server";
import { LeadStatus, LostReason } from "./schema";

/** Server functions (ADR 0037): the browser calls these, only the server calls the API. */
export const fetchLeads = createServerFn({ method: "GET" }).handler(() => source.fetchLeads());

export const fetchLead = createServerFn({ method: "GET" })
	.inputValidator(z.string())
	.handler(({ data: id }) => orNotFound(() => source.fetchLead(id)));

export const fetchTodayQueue = createServerFn({ method: "GET" }).handler(() => source.fetchTodayQueue());

export const MarkLeadInput = z.object({
	status: LeadStatus,
	lostReason: LostReason.nullable().optional(),
	note: z.string().nullable().optional(),
});
export type MarkLeadInput = z.infer<typeof MarkLeadInput>;

/** PATCH /api/leads/{id}. Backend rule errors keep their message (ADR 0012, 0020). */
export const markLead = createServerFn({ method: "POST" })
	.inputValidator(z.object({ id: z.string(), input: MarkLeadInput }))
	.handler(({ data }) => source.markLead(data.id, data.input));
