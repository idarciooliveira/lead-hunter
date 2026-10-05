import type { Tone } from "#/components/ui/chip";
import type { UsageMonth } from "./schema";

export function totalOf(m: UsageMonth): number {
	return m.apifyUsd + m.llmUsd;
}

/** Budget health, matching the thresholds in the prototype. */
export function budgetState(m: UsageMonth): { label: string; tone: Tone } {
	const share = totalOf(m) / m.budgetUsd;
	if (share > 0.8) return { label: "Perto do limite", tone: "bad" };
	if (share > 0.5) return { label: "Mais de metade gasta", tone: "warn" };
	return { label: "Dentro do orçamento", tone: "ok" };
}
