import { useState } from "react";
import { Button } from "#/components/ui/button";
import { Card } from "#/components/ui/card";
import { Chip, type Tone } from "#/components/ui/chip";
import { Input, Label } from "#/components/ui/field";
import { MultiSelect } from "#/components/ui/multi-select";
import { type CompanyProfile, MIN_CASES, SECTOR_OPTIONS, ZONE_OPTIONS } from "../schema";

type Section = { key: string; title: string; summary: string; complete: boolean; body: React.ReactNode };

export function sectionsOf(p: CompanyProfile): Section[] {
	return [
		{
			key: "services",
			title: "O que vendemos",
			summary: `${p.services.length} serviços com faixa de preço em kwanzas`,
			complete: p.services.length > 0,
			body: <Services profile={p} />,
		},
		{
			key: "target",
			title: "Clientes-alvo",
			summary: `${p.target.sectors.length} sectores, ${p.target.zones.length} zonas de Luanda`,
			complete: p.target.sectors.length > 0 && p.target.zones.length > 0,
			body: <Target profile={p} />,
		},
		{
			key: "clients",
			title: "Clientes anteriores",
			summary: `${p.pastClients.length} clientes usados para excluir duplicados`,
			complete: p.pastClients.length > 0,
			body: <PastClients profile={p} />,
		},
		{
			key: "cases",
			title: "Provas e casos",
			summary: `${p.cases.length} de ${MIN_CASES} casos`,
			complete: p.cases.length >= MIN_CASES,
			body: <Cases profile={p} />,
		},
	];
}

function Services({ profile }: { profile: CompanyProfile }) {
	return (
		<>
			<div className="flex flex-col gap-3">
				{profile.services.map((s) => (
					<div key={s.name} className="flex flex-col gap-1.5 rounded-lg border border-line bg-bg px-4 py-3">
						<div className="text-base font-semibold">{s.name}</div>
						<div className="text-mute">{s.description}</div>
						<div>
							<span className="text-mute">Faixa de preço: </span>
							<span className="font-mono">{s.priceRange}</span>
						</div>
						<div>
							<span className="text-mute">Prazo: </span>
							{s.timeline}
						</div>
					</div>
				))}
			</div>
			<Button size="sm" className="mt-3">
				Adicionar serviço
			</Button>
		</>
	);
}

function Target({ profile }: { profile: CompanyProfile }) {
	const t = profile.target;
	const [sectors, setSectors] = useState(t.sectors);
	const [zones, setZones] = useState(t.zones);
	return (
		<div className="grid grid-cols-1 gap-4">
			<div>
				<Label htmlFor="target-sectors">Sectores</Label>
				<MultiSelect
					id="target-sectors"
					options={SECTOR_OPTIONS}
					value={sectors}
					onChange={setSectors}
					placeholder="Escolher sectores"
					searchPlaceholder="Procurar ou escrever um sector"
					emptyLabel="Nenhum sector encontrado"
					createLabel={(v) => `Adicionar "${v}"`}
				/>
			</div>
			<div>
				<Label htmlFor="target-size">Dimensão</Label>
				<Input id="target-size" defaultValue={t.size} />
			</div>
			<div>
				<Label htmlFor="target-zones">Zonas de Luanda</Label>
				<MultiSelect
					id="target-zones"
					options={ZONE_OPTIONS}
					value={zones}
					onChange={setZones}
					placeholder="Escolher zonas"
					searchPlaceholder="Procurar ou escrever uma zona"
					emptyLabel="Nenhuma zona encontrada"
					createLabel={(z) => `Adicionar "${z}"`}
				/>
			</div>
			<div>
				<Label htmlFor="target-decider">Quem decide</Label>
				<Input id="target-decider" defaultValue={t.decider} />
			</div>
		</div>
	);
}

function PastClients({ profile }: { profile: CompanyProfile }) {
	return (
		<>
			<div className="mb-2 text-mute">
				{profile.pastClients.length} clientes. Leads com o mesmo nome ou telefone ficam em EXCLUDED.
			</div>
			<ul className="m-0 flex flex-col gap-1.5 pl-5">
				{profile.pastClients.map((c) => (
					<li key={c}>{c}</li>
				))}
			</ul>
			<div className="mt-3 flex gap-2">
				<Input placeholder="Nome ou telefone do cliente" aria-label="Novo cliente" />
				<Button>Adicionar</Button>
			</div>
		</>
	);
}

function Cases({ profile }: { profile: CompanyProfile }) {
	const missing = Math.max(0, MIN_CASES - profile.cases.length);
	return (
		<div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
			{profile.cases.map((c) => (
				<Card key={c.client} className="bg-bg p-3">
					<div className="font-semibold">{c.client}</div>
					<div className="text-mute">{c.summary}</div>
				</Card>
			))}
			{Array.from({ length: missing }, (_, i) => (
				<div
					// biome-ignore lint/suspicious/noArrayIndexKey: placeholders have no identity.
					key={i}
					className="flex items-center justify-center rounded-lg border border-dashed border-line2 p-3 text-mute"
				>
					Falta o {profile.cases.length + i + 1 === 2 ? "segundo" : "próximo"} caso (mínimo {MIN_CASES})
				</div>
			))}
		</div>
	);
}

/** Collapsible numbered sections; one is open at a time. */
export function ProfileSections({ sections }: { sections: Section[] }) {
	const [open, setOpen] = useState(sections[0]?.key ?? "");
	return (
		<>
			{sections.map((s, i) => {
				const tone: Tone = s.complete ? "ok" : "warn";
				const isOpen = open === s.key;
				return (
					<Card key={s.key} className="overflow-hidden">
						<button
							type="button"
							aria-expanded={isOpen}
							onClick={() => setOpen(isOpen ? "" : s.key)}
							className="flex w-full cursor-pointer items-center gap-3 border-0 bg-transparent px-4 py-3.5 text-left"
						>
							<span className="inline-flex size-6 items-center justify-center rounded-full border border-line2 font-mono text-[11px]">
								{i + 1}
							</span>
							<span className="flex-1">
								<span className="text-base font-semibold">{s.title}</span>
								<span className="block text-mute">{s.summary}</span>
							</span>
							<Chip tone={tone}>{s.complete ? "Completa" : "Incompleta"}</Chip>
						</button>
						{/* Collapsed bodies stay mounted so their unsaved edits survive. */}
						<div hidden={!isOpen} className="border-t border-line p-4">
							{s.body}
						</div>
					</Card>
				);
			})}
		</>
	);
}
