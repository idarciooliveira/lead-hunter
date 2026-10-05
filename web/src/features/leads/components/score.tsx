import { ProgressBar } from "#/components/ui/progress";
import { Tooltip } from "#/components/ui/tooltip";
import { bandOf, scoreSummary } from "../model";
import type { Lead } from "../schema";

/** Score bar and number; hovering or focusing explains the score. */
export function ScoreCell({ lead }: { lead: Lead }) {
	return (
		<Tooltip content={scoreSummary(lead)}>
			{/* biome-ignore lint/a11y/noNoninteractiveTabindex: focus shows the explanation to keyboard users. */}
			<div tabIndex={0} className="inline-flex cursor-help items-center gap-2" data-testid="score">
				<ProgressBar value={lead.score ?? 0} tone={bandOf(lead.score)} size="sm" label="Pontuação" />
				<span className="font-mono font-semibold">{lead.score ?? "–"}</span>
			</div>
		</Tooltip>
	);
}
