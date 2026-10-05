import { Link } from "@tanstack/react-router";
import { Button } from "#/components/ui/button";
import { Card } from "#/components/ui/card";
import { Chip } from "#/components/ui/chip";
import { Cost } from "#/components/ui/cost";
import { Input, Textarea } from "#/components/ui/field";
import { Kbd } from "#/components/ui/kbd";
import { OptionButton } from "#/components/ui/option-button";
import { cn } from "#/lib/utils";
import { type Answers, STEPS, STOP_RULES, yamlLines } from "../wizard";

export function StepDots({ step, onGo }: { step: number; onGo: (step: number) => void }) {
	return (
		<Card className="p-4">
			<div className="mb-3 flex gap-1">
				{STEPS.map((s, i) => (
					<button
						type="button"
						key={s.key}
						aria-label={`Pergunta ${i + 1}`}
						aria-current={i + 1 === step ? "step" : undefined}
						onClick={() => onGo(i + 1)}
						className={cn(
							"h-1.5 flex-1 cursor-pointer rounded-full border-0",
							i + 1 < step ? "bg-acc" : i + 1 === step ? "bg-acc-tx" : "bg-line",
						)}
					/>
				))}
			</div>
			<div className="font-mono text-xs text-mute">
				Pergunta {step} de {STEPS.length}
			</div>
		</Card>
	);
}

export function Question({
	step,
	answers,
	services,
	onChange,
}: {
	step: number;
	answers: Answers;
	services: string[];
	onChange: (patch: Partial<Answers>) => void;
}) {
	const q = STEPS[step - 1];
	const id = `q-${q.key}`;
	const value = answers[q.key];
	const toggle = (key: "qualify" | "disqualify" | "locations", option: string) => {
		const list = answers[key];
		onChange({ [key]: list.includes(option) ? list.filter((x) => x !== option) : [...list, option] });
	};

	return (
		<Card className="flex flex-col gap-4 p-6">
			<div>
				<h2 id={`${id}-title`} className="m-0 text-lg font-semibold tracking-[-0.01em]">
					{q.title}
				</h2>
				<div className="mt-0.5 text-mute">{q.hint}</div>
			</div>
			{q.kind === "text" && (
				<Input
					aria-labelledby={`${id}-title`}
					value={String(value)}
					onChange={(e) => onChange({ [q.key]: e.target.value })}
				/>
			)}
			{q.kind === "area" && (
				<Textarea
					aria-labelledby={`${id}-title`}
					value={String(value)}
					onChange={(e) => onChange({ [q.key]: e.target.value })}
				/>
			)}
			{q.kind === "number" && (
				<div className="flex items-center gap-2">
					<Input
						type="number"
						aria-labelledby={`${id}-title`}
						className="w-[140px] font-mono"
						value={String(value)}
						onChange={(e) => onChange({ [q.key]: Number.parseFloat(e.target.value) || 0 })}
					/>
					<span className="text-mute">{q.unit}</span>
				</div>
			)}
			{q.kind === "multi" && (
				<div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
					{q.options?.map((o) => {
						const key = q.key as "qualify" | "disqualify" | "locations";
						return (
							<OptionButton key={o} check selected={answers[key].includes(o)} onClick={() => toggle(key, o)}>
								{o}
							</OptionButton>
						);
					})}
				</div>
			)}
			{q.kind === "service" && (
				<>
					<div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
						{services.map((s) => (
							<OptionButton key={s} check selected={answers.service === s} onClick={() => onChange({ service: s })}>
								{s}
							</OptionButton>
						))}
					</div>
					<Link to="/empresa" className="text-xs text-acc-tx underline">
						Falta um serviço? Adiciona no perfil da empresa
					</Link>
				</>
			)}
			{q.kind === "goal" && (
				<>
					<div className="flex items-center gap-2">
						<Input
							type="number"
							aria-label="Meta de leads qualificados"
							className="w-[120px] font-mono"
							value={answers.goal}
							onChange={(e) => onChange({ goal: Number.parseInt(e.target.value, 10) || 0 })}
						/>
						<span className="text-mute">leads qualificados</span>
					</div>
					<div>
						<span className="mb-1.5 block text-xs font-medium text-mute">Regra de paragem</span>
						<div className="flex flex-col gap-2">
							{STOP_RULES.map((r) => (
								<OptionButton
									key={r.value}
									selected={answers.stop === r.value}
									onClick={() => onChange({ stop: r.value })}
								>
									{r.label}
								</OptionButton>
							))}
						</div>
					</div>
				</>
			)}
		</Card>
	);
}

export function WizardNav({
	step,
	onPrev,
	onNext,
	onFinish,
}: {
	step: number;
	onPrev: () => void;
	onNext: () => void;
	onFinish: () => void;
}) {
	const last = step === STEPS.length;
	return (
		<div className="flex flex-wrap items-center justify-between gap-2">
			<Button onClick={onPrev} disabled={step === 1}>
				Anterior
			</Button>
			<div className="flex items-center gap-2">
				<span className="text-xs text-mute">Dry run incluído</span>
				<Cost>$0.00</Cost>
				{last ? (
					<Button variant="primary" onClick={onFinish}>
						Criar campanha
					</Button>
				) : (
					<Button variant="primary" onClick={onNext}>
						Seguinte <Kbd onAccent>↵</Kbd>
					</Button>
				)}
			</div>
		</div>
	);
}

export function YamlImport({
	text,
	onText,
	valid,
	onValidate,
}: {
	text: string;
	onText: (text: string) => void;
	valid: boolean;
	onValidate: () => void;
}) {
	return (
		<Card className="flex flex-col gap-3 p-6">
			<div>
				<h2 className="m-0 text-base font-semibold">Colar YAML existente</h2>
				<div className="mt-0.5 text-mute">O ficheiro é validado contra o perfil da empresa antes de criar.</div>
			</div>
			<Textarea
				aria-label="YAML da campanha"
				className="min-h-[300px] font-mono text-xs"
				value={text}
				onChange={(e) => onText(e.target.value)}
			/>
			<div className="flex flex-wrap items-center gap-2">
				<Button variant="primary" onClick={onValidate}>
					Validar e criar
				</Button>
				<Cost>$0.00</Cost>
				{valid && <Chip tone="ok">12 campos lidos · serviço "Sistema de marcações" existe no perfil</Chip>}
			</div>
		</Card>
	);
}

/** Live YAML for the answers; the lines of the current question are highlighted. */
export function YamlPreview({ answers, step }: { answers: Answers; step: number }) {
	return (
		<Card className="min-w-[300px] flex-[0_1_400px] overflow-hidden">
			<div className="flex items-center justify-between border-b border-line px-4 py-3">
				<span className="text-base font-semibold">Pré-visualização YAML</span>
				<Chip>campaign.yaml</Chip>
			</div>
			<pre className="m-0 bg-bg py-3 font-mono text-xs leading-[1.7]">
				{yamlLines(answers).map((line, i) => (
					<div
						// biome-ignore lint/suspicious/noArrayIndexKey: lines have no identity beyond their position.
						key={i}
						className={cn(
							"flex gap-2 border-l-2 border-transparent py-px pr-3 whitespace-pre-wrap",
							line.step === step && "border-acc bg-acc-soft",
						)}
						style={{ paddingLeft: 12 + line.indent * 14 }}
					>
						<span className="text-acc-tx">{line.key}</span>
						<span>{line.value}</span>
					</div>
				))}
			</pre>
		</Card>
	);
}
