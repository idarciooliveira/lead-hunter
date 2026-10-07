import type { Tone } from "#/components/ui/chip";
import type { UsageMonth } from "./schema";

export function totalOf(m: UsageMonth): number {
	return m.apifyUsd + m.llmUsd;
}

/** What counts against the budget: the spend, or more when running jobs reserve some (ADR 0044). */
export function usedOf(m: UsageMonth): number {
	return Math.max(totalOf(m), m.committedUsd ?? 0);
}

/** Budget health, matching the thresholds in the prototype. */
export function budgetState(m: UsageMonth): { label: string; tone: Tone } {
	const share = usedOf(m) / m.budgetUsd;
	if (share > 0.8) return { label: "Perto do limite", tone: "bad" };
	if (share > 0.5) return { label: "Mais de metade gasta", tone: "warn" };
	return { label: "Dentro do orçamento", tone: "ok" };
}

/** `2026-10` moved by `delta` months: `shiftMonth("2026-01", -1)` is `2025-12`. */
export function shiftMonth(month: string, delta: number): string {
	const [year, m] = month.split("-").map(Number);
	const index = year * 12 + (m - 1) + delta;
	return `${Math.floor(index / 12)}-${String((index % 12) + 1).padStart(2, "0")}`;
}
