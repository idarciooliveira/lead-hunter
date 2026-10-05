import { Chip } from "#/components/ui/chip";
import { contactOf, STAGE_TONE } from "../model";
import type { Lead } from "../schema";

export function StageChip({ stage }: { stage: Lead["stage"] }) {
	return <Chip tone={STAGE_TONE[stage]}>{stage}</Chip>;
}

export function ContactChip({ lead }: { lead: Pick<Lead, "status" | "lostReason"> }) {
	const contact = contactOf(lead.status, lead.lostReason);
	return <Chip tone={contact.tone}>{contact.label}</Chip>;
}
