import { Link } from "@tanstack/react-router";
import { CallLink, WhatsAppLink } from "#/components/contact-actions";
import { Button } from "#/components/ui/button";
import { Card } from "#/components/ui/card";
import { Chip } from "#/components/ui/chip";
import { contactOf, type Outcome } from "../model";
import type { Lead } from "../schema";
import { ScoreCell } from "./score";

/** Local, unsaved progress on the queue until lead marking exists in the API. */
export type QueueState = { done: Record<string, boolean>; outcome: Record<string, Outcome> };

const QUICK: { value: Outcome; label: string }[] = [
	{ value: "CONTACTED", label: "Contactado" },
	{ value: "INTERESTED", label: "Respondeu" },
	{ value: "NOT_NOW", label: "Não agora" },
];

type Props = {
	leads: Lead[];
	state: QueueState;
	onToggle: (id: string) => void;
	onMark: (id: string, outcome: Outcome) => void;
};

export function TodayQueue({ leads, state, onToggle, onMark }: Props) {
	return (
		<>
			<Card className="hidden overflow-hidden md:block">
				<div className="overflow-x-auto">
					<ul className="m-0 min-w-[900px] list-none p-0" aria-label="Fila de contacto">
						{leads.map((lead) => {
							const outcome = state.outcome[lead.id];
							const contact = outcome ? contactOf(outcome) : contactOf(lead.status, lead.lostReason);
							return (
								<li
									key={lead.id}
									className="grid grid-cols-[28px_32px_minmax(200px,1.6fr)_140px_120px_auto] items-center gap-3 border-b border-line px-4 py-3 last:border-b-0 hover:bg-soft"
								>
									<input
										type="checkbox"
										checked={!!state.done[lead.id]}
										onChange={() => onToggle(lead.id)}
										aria-label={`Marcar ${lead.name} como feito`}
										className="size-[18px]"
									/>
									<span className="font-mono text-mute">#{lead.rank}</span>
									<div className="min-w-0">
										<Link to="/leads/$leadId" params={{ leadId: lead.id }} className="font-semibold">
											{lead.name}
										</Link>
										<div className="text-xs text-mute">
											{lead.category} · {lead.area} · <span className="font-mono">{lead.phone ?? "Sem telefone"}</span>
										</div>
									</div>
									<ScoreCell lead={lead} />
									<span>
										<Chip tone={contact.tone}>{contact.label}</Chip>
									</span>
									<div className="flex flex-wrap justify-end gap-1.5">
										<WhatsAppLink phone={lead.phone} message={lead.pitch} />
										<CallLink phone={lead.phone} />
										{QUICK.map((q) => (
											<Button
												key={q.value}
												size="sm"
												variant={outcome === q.value ? "active" : "default"}
												onClick={() => onMark(lead.id, q.value)}
											>
												{q.label}
											</Button>
										))}
									</div>
								</li>
							);
						})}
					</ul>
				</div>
			</Card>

			<ul className="m-0 flex list-none flex-col gap-2.5 p-0 md:hidden" aria-label="Fila de contacto">
				{leads.map((lead) => {
					const outcome = state.outcome[lead.id];
					const done = !!state.done[lead.id];
					return (
						<li key={lead.id}>
							<Card
								className="flex flex-col gap-2.5 rounded-xl p-3 transition-opacity"
								style={{ opacity: done ? 0.6 : 1 }}
							>
								<div className="flex items-center gap-3">
									<input
										type="checkbox"
										checked={done}
										onChange={() => onToggle(lead.id)}
										aria-label={`Marcar ${lead.name} como feito`}
										className="size-6 flex-none"
									/>
									<Link to="/leads/$leadId" params={{ leadId: lead.id }} className="block min-h-11 min-w-0 flex-1">
										<div className="font-semibold">{lead.name}</div>
										<div className="text-xs text-mute">
											{lead.category} · {lead.area}
										</div>
									</Link>
									<span className="font-mono text-lg font-semibold">{lead.score}</span>
								</div>
								<div className="flex items-center gap-2">
									<WhatsAppLink phone={lead.phone} message={lead.pitch} size="touch" className="flex-1" />
									<CallLink phone={lead.phone} size="touch-icon" label={false} />
								</div>
								<div className="flex flex-wrap items-center gap-2">
									{QUICK.map((q) => (
										<Button
											key={q.value}
											size="touch"
											className="flex-1"
											variant={outcome === q.value ? "active" : "default"}
											onClick={() => onMark(lead.id, q.value)}
										>
											{q.label}
										</Button>
									))}
								</div>
							</Card>
						</li>
					);
				})}
			</ul>
		</>
	);
}
