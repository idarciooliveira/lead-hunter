import { useSuspenseQuery } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import { ProgressBar } from "#/components/ui/progress";
import { usedOf } from "#/features/usage/model";
import { usageQuery } from "#/features/usage/queries";
import { monthName, percent, usd } from "#/lib/format";

/** This month's spend and reservations against the budget (ADR 0006, 0044). */
export function BudgetPill() {
	const { data: month } = useSuspenseQuery(usageQuery());
	const total = usedOf(month);
	return (
		<Link
			to="/uso"
			aria-label="Orçamento mensal"
			className="inline-flex h-8 items-center gap-2 rounded-full border border-line2 bg-panel px-3"
		>
			<span className="text-xs text-mute">{monthName(month.month)}</span>
			<span className="font-mono font-medium">
				{usd(total)} / ${month.budgetUsd}
			</span>
			<ProgressBar value={percent(total, month.budgetUsd)} className="w-12" size="sm" />
		</Link>
	);
}

/** One-line version for the mobile headers. */
export function BudgetText() {
	const { data: month } = useSuspenseQuery(usageQuery());
	return (
		<>
			{usd(usedOf(month))} / ${month.budgetUsd} este mês
		</>
	);
}
