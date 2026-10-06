import { z } from "zod";

/** Pipeline stage, mirrors `LeadStage` in the backend. */
export const LeadStage = z.enum(["QUALIFIED", "BELOW_CUT", "EXCLUDED"]);
export type LeadStage = z.infer<typeof LeadStage>;

/** Contact outcome, mirrors `LeadStatus` in the backend (ADR 0012). */
export const LeadStatus = z.enum([
	"NEW",
	"CONTACTED",
	"NO_ANSWER",
	"INTERESTED",
	"MEETING",
	"PROPOSAL_SENT",
	"WON",
	"LOST",
]);
export type LeadStatus = z.infer<typeof LeadStatus>;

/** Why a lead was lost: the four reasons from ADR 0012 and `NOT_NOW` from ADR 0020. */
export const LostReason = z.enum(["NO_BUDGET", "WRONG_PERSON", "HAS_SUPPLIER", "NOT_INTERESTED", "NOT_NOW"]);
export type LostReason = z.infer<typeof LostReason>;

const ScoreLine = z.object({ points: z.number().int(), reason: z.string() });
export type ScoreLine = z.infer<typeof ScoreLine>;

export const AuditResult = z.enum(["OK", "WARN", "FAIL", "PENDING"]);
export type AuditResult = z.infer<typeof AuditResult>;

export const Lead = z.object({
	id: z.string(),
	campaignSlug: z.string(),
	/** Position among non-excluded leads; null when excluded. */
	rank: z.number().int().nullable(),
	name: z.string(),
	category: z.string(),
	area: z.string(),
	address: z.string(),
	/** Google Maps link to the place; absent on sample data. */
	mapsUrl: z.string().nullable().optional(),
	rating: z.number(),
	reviewCount: z.number().int(),
	phone: z.string().nullable(),
	website: z.object({ host: z.string(), https: z.boolean() }).nullable(),
	stage: LeadStage,
	stageReason: z.string().nullable(),
	status: LeadStatus,
	lostReason: LostReason.nullable(),
	/** Null when excluded. */
	score: z.number().int().min(0).max(100).nullable(),
	breakdown: z.object({
		stage1: z.array(ScoreLine),
		/** Null until stage 2 enrichment has run. */
		stage2: z.array(ScoreLine).nullable(),
	}),
	audit: z.array(z.object({ check: z.string(), result: AuditResult, detail: z.string() })),
	/** Null until the reviews have been analysed. */
	complaints: z
		.array(z.object({ theme: z.string(), mentions: z.number().int(), quotes: z.array(z.string()) }))
		.nullable(),
	pitch: z.string(),
	note: z.string().nullable(),
});
export type Lead = z.infer<typeof Lead>;

export const LeadList = z.array(Lead);
