import type { Tone } from "#/components/ui/chip";
import type { Run } from "./schema";

export const RUN_KIND_LABEL: Record<Run["kind"], string> = {
	SCRAPE: "Scrape Apify",
	ENRICH: "Enriquecimento",
	DRY_RUN: "Dry run",
};

export const RUN_STATUS: Record<Run["status"], { label: string; tone: Tone }> = {
	RUNNING: { label: "a correr", tone: "info" },
	DONE: { label: "feito", tone: "ok" },
	FAILED: { label: "falhou", tone: "bad" },
};

export function runPlaces(run: Run): string {
	if (run.estimated) return `≈ ${run.done}`;
	return run.total === null ? String(run.done) : `${run.done}/${run.total}`;
}
