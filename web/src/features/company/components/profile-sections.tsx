import { useState } from "react";
import { Button } from "#/components/ui/button";
import { Card } from "#/components/ui/card";
import { Chip, type Tone } from "#/components/ui/chip";
import { Input, Label } from "#/components/ui/field";
import { MultiSelect } from "#/components/ui/multi-select";
import { type CompanyProfile, MIN_CASES, ZONE_OPTIONS } from "../schema";

type Section = { key: string; title: string; summary: string; complete: boolean; body: React.ReactNode };

export function sectionsOf(p: CompanyProfile): Section[] {
	return [
		{
			key: "services",
			title: "O que vendemos",
			summary: `${p.services.length} serviços com preço e prazo`,
			complete: p.services.length > 0,
			body: <Services profile={p} />,
		},
		{
			key: "area",
			title: "Onde trabalhamos",
			summary: `${p.area.length} zonas de Luanda`,
			complete: p.area.length > 0,
			body: <Area profile={p} />,
		},
		{
			key: "clients",
			title: "Clientes actuais",
			summary: `${p.clients.length} clientes usados para excluir duplicados`,
			complete: p.clients.length > 0,
			body: <Clients profile={p} />,
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
						<div>
							<span className="text-mute">Preço: </span>
							<span className="font-mono">{s.price ?? "por definir"}</span>
						</div>
						<div>
							<span className="text-mute">Prazo: </span>
							{s.deliveryTime ?? "por definir"}
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

function Area({ profile }: { profile: CompanyProfile }) {
	const [zones, setZones] = useState(profile.area);
	return (
		<div>
			<Label htmlFor="area-zones">Zonas de Luanda</Label>
			<MultiSelect
				id="area-zones"
				options={ZONE_OPTIONS}
				value={zones}
				onChange={setZones}
				placeholder="Escolher zonas"
				searchPlaceholder="Procurar ou escrever uma zona"
				emptyLabel="Nenhuma zona encontrada"
				createLabel={(z) => `Adicionar "${z}"`}
			/>
		</div>
	);
}

function Clients({ profile }: { profile: CompanyProfile }) {
	return (
		<>
			<div className="mb-2 text-mute">
				{profile.clients.length} clientes. Leads com o mesmo nome ou telefone ficam em EXCLUDED.
			</div>
			<ul className="m-0 flex flex-col gap-1.5 pl-5">
				{profile.clients.map((c) => (
					<li key={c.name}>
						{c.name}
						{c.phone && <span className="font-mono text-mute"> · {c.phone}</span>}
					</li>
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
			{profile.cases.map((c, i) => (
				// biome-ignore lint/suspicious/noArrayIndexKey: cases have no id and the client may be anonymous.
				<Card key={i} className="bg-bg p-3">
					<div className="font-semibold">{c.mayName && c.client ? c.client : "Cliente anónimo"}</div>
					{c.sector && <div className="text-xs text-mute">{c.sector}</div>}
					<div className="text-mute">{c.result}</div>
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
