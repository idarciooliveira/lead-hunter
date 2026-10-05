import type { Tone } from "#/components/ui/chip";
import type { CampaignState } from "./schema";

export const CAMPAIGN_STATE: Record<CampaignState, { label: string; tone: Tone }> = {
	DRAFT: { label: "Rascunho", tone: "neutral" },
	RUNNING: { label: "A correr", tone: "info" },
	ENRICHING: { label: "A enriquecer", tone: "info" },
	DONE: { label: "Concluída", tone: "ok" },
	FAILED: { label: "Execução falhou", tone: "bad" },
};
