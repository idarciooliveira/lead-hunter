import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Page, PageHeader } from "#/components/page-header";
import { StatBar } from "#/components/stat-bar";
import { Button } from "#/components/ui/button";
import { Chip } from "#/components/ui/chip";
import { ProfileSections, sectionsOf } from "#/features/company/components/profile-sections";
import { companyQuery } from "#/features/company/queries";
import { percent } from "#/lib/format";

export const Route = createFileRoute("/empresa")({
	loader: ({ context }) => context.queryClient.ensureQueryData(companyQuery()),
	head: () => ({ meta: [{ title: "Empresa · Lead Hunter" }] }),
	component: CompanyPage,
});

function CompanyPage() {
	const { data: profile } = useSuspenseQuery(companyQuery());
	const sections = sectionsOf(profile);
	const complete = sections.filter((s) => s.complete).length;
	const share = percent(complete, sections.length);
	const firstGap = sections.find((s) => !s.complete);
	return (
		<Page>
			<PageHeader
				title="Empresa"
				subtitle="O perfil que as campanhas usam para propor serviços e excluir clientes que já tens."
				actions={<Button variant="primary">Guardar perfil</Button>}
			/>
			<StatBar
				value={`${Math.round(share)}%`}
				caption={`perfil completo, ${complete} de ${sections.length} secções`}
				percent={share}
				label="Perfil completo"
				aside={firstGap && <Chip tone="warn">Falta completar "{firstGap.title}"</Chip>}
			/>
			<ProfileSections sections={sections} />
		</Page>
	);
}
