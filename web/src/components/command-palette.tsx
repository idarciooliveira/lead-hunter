import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { CommandDialog, CommandEmpty, CommandItem } from "#/components/ui/command";
import { Kbd } from "#/components/ui/kbd";
import { leadsQuery } from "#/features/leads/queries";
import { useTheme } from "#/lib/theme";
import { NAV, STATES_NAV } from "./nav";

type Item = { label: string; group: string; hint: string; run: () => void };

export function CommandPalette({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
	const navigate = useNavigate();
	const { toggle } = useTheme();
	const { data: leads = [] } = useQuery({ ...leadsQuery(), enabled: open });

	const items: Item[] = [
		...[...NAV, STATES_NAV].map((n) => ({
			label: n.to === "/estados" ? n.label : `Ir para ${n.label}`,
			group: "Navegar",
			hint: n.keys,
			run: () => navigate({ to: n.to }),
		})),
		{ label: "Nova campanha", group: "Acção", hint: "N", run: () => navigate({ to: "/campanhas/nova" }) },
		{ label: "Alternar tema claro e escuro", group: "Acção", hint: "T", run: toggle },
		...leads.slice(0, 6).map((l) => ({
			label: `Abrir lead: ${l.name}`,
			group: "Lead",
			hint: "↵",
			run: () => navigate({ to: "/leads/$leadId", params: { leadId: l.id } }),
		})),
	];

	return (
		<CommandDialog open={open} onOpenChange={onOpenChange} placeholder="Escreve um comando ou o nome de um lead">
			<CommandEmpty>Nada encontrado.</CommandEmpty>
			{items.map((item) => (
				<CommandItem
					key={item.label}
					value={item.label}
					onSelect={() => {
						onOpenChange(false);
						item.run();
					}}
				>
					<span className="flex-1">{item.label}</span>
					<span className="text-xs text-mute">{item.group}</span>
					<Kbd>{item.hint}</Kbd>
				</CommandItem>
			))}
		</CommandDialog>
	);
}
