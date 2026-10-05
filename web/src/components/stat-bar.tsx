import type * as React from "react";
import { Card } from "#/components/ui/card";
import { ProgressBar } from "#/components/ui/progress";

/** A big number, a progress bar and a note on the right; heads Hoje, campaigns, company and usage. */
export function StatBar({
	value,
	caption,
	percent,
	aside,
	label,
}: {
	value: React.ReactNode;
	caption: React.ReactNode;
	percent: number;
	aside?: React.ReactNode;
	label: string;
}) {
	return (
		<Card className="flex flex-wrap items-center gap-4 p-4">
			<div className="min-w-[180px]">
				<span className="font-mono text-xl font-semibold">{value}</span> <span className="text-mute">{caption}</span>
			</div>
			<div className="min-w-[200px] flex-1">
				<ProgressBar value={percent} label={label} />
			</div>
			{aside}
		</Card>
	);
}
