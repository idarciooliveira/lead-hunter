import { useSuspenseQuery } from "@tanstack/react-query";
import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Page, PageHeader } from "#/components/page-header";
import { Button } from "#/components/ui/button";
import { Card } from "#/components/ui/card";
import { Segmented } from "#/components/ui/segmented";
import { Question, StepDots, WizardNav, YamlImport, YamlPreview } from "#/features/campaigns/components/wizard";
import { useCreateCampaign } from "#/features/campaigns/queries";
import { type Answers, campaignFileFromAnswers, SAMPLE_ANSWERS, SAMPLE_YAML, STEPS } from "#/features/campaigns/wizard";
import { companyQuery } from "#/features/company/queries";

export const Route = createFileRoute("/_app/campanhas/nova")({
	loader: ({ context }) => context.queryClient.ensureQueryData(companyQuery()),
	head: () => ({ meta: [{ title: "Nova campanha · Lead Hunter" }] }),
	component: NewCampaignPage,
});

function NewCampaignPage() {
	const { data: company } = useSuspenseQuery(companyQuery());
	const navigate = useNavigate();
	const [mode, setMode] = useState<"guided" | "import">("guided");
	const [step, setStep] = useState(1);
	const [answers, setAnswers] = useState<Answers>(SAMPLE_ANSWERS);
	const [yaml, setYaml] = useState(SAMPLE_YAML);
	const [yamlValid, setYamlValid] = useState(false);
	const create = useCreateCampaign();
	const [saveError, setSaveError] = useState<string | null>(null);
	const [created, setCreated] = useState<{ slug: string; name: string; warnings: string[] } | null>(null);
	// The guided answers become POST /api/campaigns. A clean save lands on the
	// new page; warnings stay here first, since the detail page never shows them.
	const finish = () => {
		setSaveError(null);
		create.mutate(campaignFileFromAnswers(answers), {
			onSuccess: ({ saved, warnings }) => {
				if (warnings.length > 0) setCreated({ slug: saved.slug, name: saved.name, warnings });
				else navigate({ to: "/campanhas/$slug", params: { slug: saved.slug } });
			},
			onError: (e) => setSaveError(e.message),
		});
	};

	return (
		<Page>
			<PageHeader
				before={
					<Button size="sm" asChild>
						<Link to="/campanhas">Voltar às campanhas</Link>
					</Button>
				}
				title="Nova campanha"
				subtitle={`${STEPS.length} perguntas. O YAML à direita actualiza enquanto respondes.`}
				actions={
					<Segmented
						label="Modo"
						value={mode}
						onChange={setMode}
						options={[
							{ value: "guided", label: "Guiado" },
							{ value: "import", label: "Importar YAML" },
						]}
					/>
				}
			/>
			<div className="flex flex-wrap items-start gap-4">
				<div className="flex min-w-0 flex-[1_1_520px] flex-col gap-4">
					{mode === "guided" ? (
						<>
							<StepDots step={step} onGo={setStep} />
							<Question
								step={step}
								answers={answers}
								services={company?.services.map((s) => s.name) ?? []}
								onChange={(patch) => setAnswers((a) => ({ ...a, ...patch }))}
							/>
							<WizardNav
								step={step}
								onPrev={() => setStep((s) => Math.max(1, s - 1))}
								onNext={() => setStep((s) => Math.min(STEPS.length, s + 1))}
								onFinish={finish}
								saving={create.isPending}
							/>
							{saveError && (
								<div role="alert" className="text-sm text-bad">
									{saveError}
								</div>
							)}
							{created && (
								<Card className="flex flex-col gap-2 p-4">
									<span className="text-sm font-semibold">Campanha criada com avisos</span>
									<ul className="m-0 flex flex-col gap-1 pl-5 text-sm text-mute">
										{created.warnings.map((w) => (
											<li key={w}>{w}</li>
										))}
									</ul>
									<div>
										<Button size="sm" variant="primary" asChild>
											<Link to="/campanhas/$slug" params={{ slug: created.slug }}>
												Ver {created.name}
											</Link>
										</Button>
									</div>
								</Card>
							)}
						</>
					) : (
						<YamlImport
							text={yaml}
							onText={(t) => {
								setYaml(t);
								setYamlValid(false);
							}}
							valid={yamlValid}
							onValidate={() => setYamlValid(true)}
						/>
					)}
				</div>
				<YamlPreview answers={answers} step={mode === "guided" ? step : 0} />
			</div>
		</Page>
	);
}
