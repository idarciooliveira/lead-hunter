import { useState } from "react";
import { Button } from "#/components/ui/button";
import { Card } from "#/components/ui/card";
import { Chip, type Tone } from "#/components/ui/chip";
import { Input, Label, NativeSelect, Textarea } from "#/components/ui/field";
import { MultiSelect } from "#/components/ui/multi-select";
import { type CompanyProfile, MIN_CASES, ZONE_OPTIONS } from "../schema";

type Section = { key: string; title: string; summary: string; complete: boolean; body: React.ReactNode };
type Edit = (next: CompanyProfile) => void;

const blank = (v: string | null) => !v?.trim();
/** Empty inputs become null, which is how the schema holds an unanswered question. */
const orNull = (v: string) => (v === "" ? null : v);

export function sectionsOf(p: CompanyProfile, edit: Edit): Section[] {
	return [
		{
			key: "company",
			title: "A empresa",
			summary: p.name.trim() ? p.name : "Sem nome",
			complete: !blank(p.name) && !blank(p.intro),
			body: <Identity profile={p} edit={edit} />,
		},
		{
			key: "services",
			title: "O que vendemos",
			summary: `${p.services.length} serviços com preço e prazo`,
			complete: p.services.length > 0 && p.services.every((s) => !blank(s.name) && !blank(s.price)),
			body: <Services profile={p} edit={edit} />,
		},
		{
			key: "area",
			title: "Onde trabalhamos",
			summary: `${p.area.length} zonas de Luanda`,
			complete: p.area.length > 0,
			body: <Area profile={p} edit={edit} />,
		},
		{
			key: "clients",
			title: "Clientes actuais",
			summary: `${p.clients.length} clientes usados para excluir duplicados`,
			complete: p.clients.length > 0,
			body: <Clients profile={p} edit={edit} />,
		},
		{
			key: "cases",
			title: "Provas e casos",
			summary: `${p.cases.length} de ${MIN_CASES} casos`,
			complete: p.cases.length >= MIN_CASES,
			body: <Cases profile={p} edit={edit} />,
		},
	];
}

/** Replaces the item at `index`, or drops it when `item` is null. */
function replaceAt<T>(list: T[], index: number, item: T | null): T[] {
	return item === null ? list.filter((_, i) => i !== index) : list.map((x, i) => (i === index ? item : x));
}

function Identity({ profile, edit }: { profile: CompanyProfile; edit: Edit }) {
	return (
		<div className="flex flex-col gap-3">
			<div>
				<Label htmlFor="company-name">Nome</Label>
				<Input id="company-name" value={profile.name} onChange={(e) => edit({ ...profile, name: e.target.value })} />
			</div>
			<div>
				<Label htmlFor="company-intro">Apresentação numa frase</Label>
				<Textarea
					id="company-intro"
					value={profile.intro ?? ""}
					onChange={(e) => edit({ ...profile, intro: orNull(e.target.value) })}
				/>
			</div>
		</div>
	);
}

function Services({ profile, edit }: { profile: CompanyProfile; edit: Edit }) {
	const setService = (i: number, service: CompanyProfile["services"][number] | null) => {
		const old = profile.services[i];
		// The entry offer is matched by name, so it follows a rename and clears on removal.
		const entryOffer = old && profile.entryOffer === old.name ? (service ? service.name : null) : profile.entryOffer;
		edit({ ...profile, services: replaceAt(profile.services, i, service), entryOffer });
	};
	return (
		<>
			<div className="flex flex-col gap-3">
				{profile.services.map((s, i) => (
					// biome-ignore lint/suspicious/noArrayIndexKey: names are editable, so they cannot be keys.
					<div key={i} className="flex flex-col gap-2 rounded-lg border border-line bg-bg px-4 py-3">
						<div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
							<div>
								<Label htmlFor={`service-name-${i}`}>Serviço</Label>
								<Input
									id={`service-name-${i}`}
									value={s.name}
									onChange={(e) => setService(i, { ...s, name: e.target.value })}
								/>
							</div>
							<div>
								<Label htmlFor={`service-price-${i}`}>Preço</Label>
								<Input
									id={`service-price-${i}`}
									className="font-mono"
									value={s.price ?? ""}
									placeholder="150 000 a 300 000 Kz"
									onChange={(e) => setService(i, { ...s, price: orNull(e.target.value) })}
								/>
							</div>
							<div>
								<Label htmlFor={`service-time-${i}`}>Prazo</Label>
								<Input
									id={`service-time-${i}`}
									value={s.deliveryTime ?? ""}
									placeholder="2 a 4 semanas"
									onChange={(e) => setService(i, { ...s, deliveryTime: orNull(e.target.value) })}
								/>
							</div>
						</div>
						<div className="flex justify-end">
							<Button size="sm" aria-label={`Remover serviço ${s.name}`} onClick={() => setService(i, null)}>
								Remover
							</Button>
						</div>
					</div>
				))}
			</div>
			<Button
				size="sm"
				className="mt-3"
				onClick={() =>
					edit({ ...profile, services: [...profile.services, { name: "", price: null, deliveryTime: null }] })
				}
			>
				Adicionar serviço
			</Button>
			{profile.services.length > 0 && (
				<div className="mt-4 max-w-sm">
					<Label htmlFor="entry-offer">Oferta de entrada</Label>
					<NativeSelect
						id="entry-offer"
						value={profile.entryOffer ?? ""}
						onChange={(e) => edit({ ...profile, entryOffer: orNull(e.target.value) })}
					>
						<option value="">Escolher serviço</option>
						{profile.services
							.filter((s) => s.name.trim())
							.map((s, i) => (
								// biome-ignore lint/suspicious/noArrayIndexKey: two services may share a name while editing.
								<option key={i} value={s.name}>
									{s.name}
								</option>
							))}
					</NativeSelect>
				</div>
			)}
		</>
	);
}

function Area({ profile, edit }: { profile: CompanyProfile; edit: Edit }) {
	return (
		<div>
			<Label htmlFor="area-zones">Zonas de Luanda</Label>
			<MultiSelect
				id="area-zones"
				options={ZONE_OPTIONS}
				value={profile.area}
				onChange={(area) => edit({ ...profile, area })}
				placeholder="Escolher zonas"
				searchPlaceholder="Procurar ou escrever uma zona"
				emptyLabel="Nenhuma zona encontrada"
				createLabel={(z) => `Adicionar "${z}"`}
			/>
		</div>
	);
}

function Clients({ profile, edit }: { profile: CompanyProfile; edit: Edit }) {
	const [name, setName] = useState("");
	const [phone, setPhone] = useState("");
	const add = () => {
		if (!name.trim()) return;
		edit({ ...profile, clients: [...profile.clients, { name: name.trim(), phone: orNull(phone.trim()) }] });
		setName("");
		setPhone("");
	};
	return (
		<>
			<div className="mb-2 text-mute">
				{profile.clients.length} clientes. Leads com o mesmo nome ou telefone ficam em EXCLUDED.
			</div>
			<ul className="m-0 flex list-none flex-col gap-2 p-0">
				{profile.clients.map((c, i) => (
					// biome-ignore lint/suspicious/noArrayIndexKey: names are editable, so they cannot be keys.
					<li key={i} className="flex gap-2">
						<Input
							aria-label={`Nome do cliente ${i + 1}`}
							value={c.name}
							onChange={(e) =>
								edit({ ...profile, clients: replaceAt(profile.clients, i, { ...c, name: e.target.value }) })
							}
						/>
						<Input
							aria-label={`Telefone do cliente ${i + 1}`}
							className="font-mono"
							value={c.phone ?? ""}
							placeholder="+244..."
							onChange={(e) =>
								edit({ ...profile, clients: replaceAt(profile.clients, i, { ...c, phone: orNull(e.target.value) }) })
							}
						/>
						<Button
							aria-label={`Remover cliente ${c.name}`}
							onClick={() => edit({ ...profile, clients: replaceAt(profile.clients, i, null) })}
						>
							Remover
						</Button>
					</li>
				))}
			</ul>
			<form
				className="mt-3 flex gap-2"
				onSubmit={(e) => {
					e.preventDefault();
					add();
				}}
			>
				<Input
					placeholder="Nome do cliente"
					aria-label="Novo cliente"
					value={name}
					onChange={(e) => setName(e.target.value)}
				/>
				<Input
					placeholder="Telefone (opcional)"
					aria-label="Telefone do novo cliente"
					className="font-mono"
					value={phone}
					onChange={(e) => setPhone(e.target.value)}
				/>
				<Button type="submit">Adicionar</Button>
			</form>
		</>
	);
}

function Cases({ profile, edit }: { profile: CompanyProfile; edit: Edit }) {
	const missing = Math.max(0, MIN_CASES - profile.cases.length);
	const setCase = (i: number, c: CompanyProfile["cases"][number] | null) =>
		edit({ ...profile, cases: replaceAt(profile.cases, i, c) });
	return (
		<>
			<div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
				{profile.cases.map((c, i) => (
					// biome-ignore lint/suspicious/noArrayIndexKey: cases have no id and the client may be anonymous.
					<Card key={i} className="flex flex-col gap-2 bg-bg p-3">
						<div>
							<Label htmlFor={`case-sector-${i}`}>Sector</Label>
							<Input
								id={`case-sector-${i}`}
								value={c.sector ?? ""}
								onChange={(e) => setCase(i, { ...c, sector: orNull(e.target.value) })}
							/>
						</div>
						<div>
							<Label htmlFor={`case-client-${i}`}>Cliente</Label>
							<Input
								id={`case-client-${i}`}
								value={c.client ?? ""}
								onChange={(e) => setCase(i, { ...c, client: orNull(e.target.value) })}
							/>
						</div>
						<div>
							<Label htmlFor={`case-problem-${i}`}>Problema</Label>
							<Textarea
								id={`case-problem-${i}`}
								className="min-h-[60px]"
								value={c.problem ?? ""}
								onChange={(e) => setCase(i, { ...c, problem: orNull(e.target.value) })}
							/>
						</div>
						<div>
							<Label htmlFor={`case-built-${i}`}>O que foi feito</Label>
							<Textarea
								id={`case-built-${i}`}
								className="min-h-[60px]"
								value={c.built ?? ""}
								onChange={(e) => setCase(i, { ...c, built: orNull(e.target.value) })}
							/>
						</div>
						<div>
							<Label htmlFor={`case-result-${i}`}>Resultado, com um número</Label>
							<Textarea
								id={`case-result-${i}`}
								className="min-h-[60px]"
								value={c.result ?? ""}
								onChange={(e) => setCase(i, { ...c, result: orNull(e.target.value) })}
							/>
						</div>
						<div className="flex items-center justify-between">
							<label className="flex items-center gap-2 text-xs text-mute">
								<input
									type="checkbox"
									checked={c.mayName}
									onChange={(e) => setCase(i, { ...c, mayName: e.target.checked })}
								/>
								Podemos usar o nome do cliente
							</label>
							<Button size="sm" aria-label={`Remover caso ${i + 1}`} onClick={() => setCase(i, null)}>
								Remover
							</Button>
						</div>
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
			<Button
				size="sm"
				className="mt-3"
				onClick={() =>
					edit({
						...profile,
						cases: [
							...profile.cases,
							{ sector: null, client: null, problem: null, built: null, result: null, mayName: false },
						],
					})
				}
			>
				Adicionar caso
			</Button>
		</>
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
