import { useQuery, useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link, notFound, useNavigate } from "@tanstack/react-router";
import { useMemo } from "react";
import { Button } from "#/components/ui/button";
import { Kbd } from "#/components/ui/kbd";
import {
	ContactOutcome,
	LeadHero,
	ReviewComplaints,
	ScoreBreakdown,
	SuggestedMessage,
	WebsiteAudit,
} from "#/features/leads/components/lead-detail";
import { leadQuery, leadsQuery } from "#/features/leads/queries";
import { NotFoundError } from "#/lib/fake-api";
import { whatsappUrl } from "#/lib/format";
import { useHotkeys } from "#/lib/use-hotkeys";

export const Route = createFileRoute("/leads/$leadId")({
	loader: async ({ context, params }) => {
		try {
			const lead = await context.queryClient.ensureQueryData(leadQuery(params.leadId));
			return { name: lead.name };
		} catch (e) {
			if (e instanceof NotFoundError) throw notFound();
			throw e;
		}
	},
	head: ({ loaderData }) => ({ meta: [{ title: `${loaderData?.name ?? "Lead"} · Lead Hunter` }] }),
	component: LeadPage,
});

function LeadPage() {
	const { leadId } = Route.useParams();
	const { data: lead } = useSuspenseQuery(leadQuery(leadId));
	const { data: leads = [] } = useQuery(leadsQuery());
	const navigate = useNavigate();

	const { prev, next, bindings } = useMemo(() => {
		const index = leads.findIndex((l) => l.id === lead.id);
		const step = (delta: number) => () => {
			if (!leads.length) return;
			const target = leads[(index + delta + leads.length) % leads.length];
			navigate({ to: "/leads/$leadId", params: { leadId: target.id } });
		};
		const openWhatsApp = () => {
			if (lead.phone) window.open(whatsappUrl(lead.phone, lead.pitch), "_blank", "noopener");
		};
		return { prev: step(-1), next: step(1), bindings: { k: step(-1), j: step(1), w: openWhatsApp } };
	}, [lead, leads, navigate]);
	useHotkeys(bindings);

	return (
		<div className="flex flex-col gap-4">
			<div className="flex flex-wrap items-start justify-between gap-4">
				<Button size="sm" asChild>
					<Link to="/leads">Voltar aos leads</Link>
				</Button>
				<div className="flex gap-2">
					<Button size="sm" onClick={prev}>
						Anterior <Kbd>K</Kbd>
					</Button>
					<Button size="sm" onClick={next}>
						Seguinte <Kbd>J</Kbd>
					</Button>
				</div>
			</div>
			<LeadHero lead={lead} />
			<div className="flex flex-wrap items-start gap-4">
				<div className="flex min-w-0 flex-[1.5_1_480px] flex-col gap-4">
					<ScoreBreakdown lead={lead} />
					<WebsiteAudit lead={lead} />
					<ReviewComplaints lead={lead} />
				</div>
				<div className="flex min-w-0 flex-[1_1_340px] flex-col gap-4">
					<SuggestedMessage lead={lead} />
					<ContactOutcome key={lead.id} lead={lead} />
				</div>
			</div>
		</div>
	);
}
