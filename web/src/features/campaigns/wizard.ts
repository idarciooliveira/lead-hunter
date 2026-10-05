/** The 11 campaign questions (ADR 0018, 0019) and the YAML preview built from the answers. */

export type Answers = {
	name: string;
	sector: string;
	problem: string;
	service: string;
	qualify: string[];
	disqualify: string[];
	minReviews: number;
	goal: number;
	stop: "goal" | "budget" | "all";
	terms: string;
	locations: string[];
	limitUsd: number;
};

type Kind = "text" | "area" | "service" | "multi" | "number" | "goal";

export type Step = {
	title: string;
	hint: string;
	kind: Kind;
	key: keyof Answers;
	options?: string[];
	unit?: string;
};

export const STEPS: Step[] = [
	{
		title: "Como se chama a campanha?",
		hint: "Aparece na lista de campanhas e nos relatórios de custo.",
		kind: "text",
		key: "name",
	},
	{
		title: "Que sector queres atacar?",
		hint: "Um sector por campanha dá pesquisas mais limpas.",
		kind: "text",
		key: "sector",
	},
	{
		title: "Que problema achamos que eles têm?",
		hint: "É a aposta da campanha. O Stage 2 procura provas disto.",
		kind: "area",
		key: "problem",
	},
	{
		title: "Que serviço vamos propor?",
		hint: "Só aparecem serviços do perfil da empresa.",
		kind: "service",
		key: "service",
	},
	{
		title: "Que sinais qualificam um lead?",
		hint: "Cada sinal presente soma pontos na pontuação.",
		kind: "multi",
		key: "qualify",
		options: [
			"Sem website",
			"Website sem HTTPS",
			"Website desactualizado",
			"Sem marcação online",
			"Queixas de demora nas reviews",
			"Só tem Instagram ou Facebook",
		],
	},
	{
		title: "Que sinais desqualificam?",
		hint: "Um destes põe o lead em EXCLUDED.",
		kind: "multi",
		key: "disqualify",
		options: [
			"Já tem sistema de marcações",
			"Faz parte de uma cadeia",
			"Cliente existente",
			"Encerrado definitivamente",
			"Unidade pública",
		],
	},
	{
		title: "Quantas avaliações, no mínimo?",
		hint: "Abaixo disto o negócio é pequeno demais para valer a conversa.",
		kind: "number",
		key: "minReviews",
		unit: "avaliações",
	},
	{
		title: "Qual é a meta e quando paramos?",
		hint: "A regra de paragem evita gastar além do necessário.",
		kind: "goal",
		key: "goal",
	},
	{
		title: "Que termos pesquisamos no Google Maps?",
		hint: "Separados por vírgula. Cada termo e localização gera uma pesquisa.",
		kind: "area",
		key: "terms",
	},
	{
		title: "Em que zonas?",
		hint: "Mais zonas, mais lugares, mais custo.",
		kind: "multi",
		key: "locations",
		options: [
			"Talatona",
			"Kilamba",
			"Belas",
			"Maianga",
			"Ingombota",
			"Miramar",
			"Viana",
			"Cazenga",
			"Zango",
			"Alvalade",
		],
	},
	{
		title: "Qual é o limite de custo Apify?",
		hint: "Acima disto a execução pede --allow-over-limit.",
		kind: "number",
		key: "limitUsd",
		unit: "dólares",
	},
];

export const STOP_RULES: { value: Answers["stop"]; label: string; yaml: string }[] = [
	{ value: "goal", label: "Parar ao atingir a meta", yaml: "goal_reached" },
	{ value: "budget", label: "Parar ao gastar o limite de custo", yaml: "budget_reached" },
	{ value: "all", label: "Percorrer todas as zonas", yaml: "all_locations" },
];

export const SAMPLE_ANSWERS: Answers = {
	name: "Clínicas Talatona",
	sector: "Clínicas privadas",
	problem: "Marcam consultas só por telefone e perdem pacientes pela demora.",
	service: "Sistema de marcações",
	qualify: ["Sem marcação online", "Queixas de demora nas reviews"],
	disqualify: ["Cliente existente"],
	minReviews: 50,
	goal: 100,
	stop: "goal",
	terms: "clínica privada talatona, clínica kilamba",
	locations: ["Talatona", "Kilamba"],
	limitUsd: 1.5,
};

export const SAMPLE_YAML = `name: Clínicas Talatona
sector: Clínicas privadas
service: Sistema de marcações
min_reviews: 50
goal:
  qualified_leads: 100
  stop: goal_reached
locations:
  - Talatona
  - Kilamba
limits:
  apify_usd: 2.00`;

export type YamlLine = { indent: number; key: string; value: string; step: number };

/** The preview on the right of the wizard. Each line knows which question wrote it. */
export function yamlLines(a: Answers): YamlLine[] {
	const lines: YamlLine[] = [];
	const add = (indent: number, key: string, value: string, step: number) => lines.push({ indent, key, value, step });
	const list = (indent: number, items: string[], step: number) => {
		for (const item of items) add(indent, "- ", item, step);
	};
	add(0, "name: ", `"${a.name}"`, 1);
	add(0, "sector: ", `"${a.sector}"`, 2);
	add(0, "problem: ", `"${a.problem}"`, 3);
	add(0, "service: ", `"${a.service}"`, 4);
	add(0, "signals:", "", 5);
	add(1, "qualify:", a.qualify.length ? "" : " []", 5);
	list(2, a.qualify, 5);
	add(1, "disqualify:", a.disqualify.length ? "" : " []", 6);
	list(2, a.disqualify, 6);
	add(0, "min_reviews: ", String(a.minReviews), 7);
	add(0, "goal:", "", 8);
	add(1, "qualified_leads: ", String(a.goal), 8);
	add(1, "stop: ", STOP_RULES.find((r) => r.value === a.stop)?.yaml ?? "goal_reached", 8);
	add(0, "search_terms:", "", 9);
	list(
		1,
		a.terms
			.split(",")
			.map((t) => t.trim())
			.filter(Boolean),
		9,
	);
	add(0, "locations:", a.locations.length ? "" : " []", 10);
	list(1, a.locations, 10);
	add(0, "limits:", "", 11);
	add(1, "apify_usd: ", Number(a.limitUsd).toFixed(2), 11);
	return lines;
}
