import { Link } from "@tanstack/react-router";
import { WhatsAppLink } from "#/components/contact-actions";
import { Card } from "#/components/ui/card";
import { ProgressBar } from "#/components/ui/progress";
import { rating } from "#/lib/format";
import { bandOf, scoreSummary } from "../model";
import type { Lead } from "../schema";
import { ContactChip, StageChip } from "./status-chips";

/** The mobile ranking: one card per lead with a 44px WhatsApp button. */
export function LeadCards({ leads }: { leads: Lead[] }) {
	return (
		<ul className="m-0 flex list-none flex-col gap-2.5 p-0" aria-label="Leads">
			{leads.map((lead) => (
				<li key={lead.id}>
					<Card className="flex flex-col gap-2 rounded-xl p-3">
						<div className="flex items-start gap-3">
							<span className="w-6 pt-0.5 font-mono text-mute">#{lead.rank ?? "–"}</span>
							<Link to="/leads/$leadId" params={{ leadId: lead.id }} className="block min-h-11 min-w-0 flex-1">
								<div className="font-semibold">{lead.name}</div>
								<div className="text-xs text-mute">
									{lead.category} · {lead.area}
								</div>
								<div className="mt-0.5 font-mono text-xs">
									{rating(lead.rating)} <span className="text-mute">({lead.reviewCount} avaliações)</span>
								</div>
							</Link>
							<WhatsAppLink
								phone={lead.phone}
								message={lead.pitch}
								iconOnly
								size="touch-icon"
								className="border-acc bg-acc text-on-acc hover:bg-acc-h"
							/>
						</div>
						<div className="flex flex-wrap items-center gap-2">
							<ProgressBar
								value={lead.score ?? 0}
								tone={bandOf(lead.score)}
								size="sm"
								className="w-14"
								label="Pontuação"
							/>
							<span className="font-mono font-semibold">{lead.score ?? "–"}</span>
							<StageChip stage={lead.stage} />
							<ContactChip lead={lead} />
						</div>
						<div className="text-xs text-mute">{scoreSummary(lead)}</div>
					</Card>
				</li>
			))}
		</ul>
	);
}
