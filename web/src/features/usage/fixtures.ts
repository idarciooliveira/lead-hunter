import type { UsageMonth } from "./schema";

/** Newest month first. */
export const USAGE: UsageMonth[] = [
	{
		month: "2026-10",
		budgetUsd: 10,
		committedUsd: null,
		apifyUsd: 2.61,
		llmUsd: 0.81,
		byCampaign: [
			{ campaign: "Clínicas Talatona", usd: 1.12 },
			{ campaign: "Restaurantes Maianga", usd: 0.97 },
			{ campaign: "Escolas Viana", usd: 0.78 },
			{ campaign: "Oficinas Zango", usd: 0.55 },
		],
		events: [
			{
				at: "2026-10-05T10:02:00+01:00",
				campaign: "Clínicas Talatona",
				source: "LLM",
				detail: "Enriquecimento de 14 websites e reviews",
				costUsd: 0.28,
			},
			{
				at: "2026-10-05T09:41:00+01:00",
				campaign: "Clínicas Talatona",
				source: "APIFY",
				detail: "Scrape de 142 lugares",
				costUsd: 0.84,
			},
			{
				at: "2026-10-03T16:20:00+01:00",
				campaign: "Restaurantes Maianga",
				source: "LLM",
				detail: "Auditoria de 22 websites",
				costUsd: 0.41,
			},
			{
				at: "2026-10-02T11:05:00+01:00",
				campaign: "Oficinas Zango",
				source: "APIFY",
				detail: "Execução falhou, sem custo",
				costUsd: 0,
			},
			{
				at: "2026-10-01T14:30:00+01:00",
				campaign: "Escolas Viana",
				source: "APIFY",
				detail: "Scrape de 118 lugares",
				costUsd: 0.78,
			},
		],
	},
	{
		month: "2026-09",
		budgetUsd: 10,
		committedUsd: null,
		apifyUsd: 5.2,
		llmUsd: 1.7,
		byCampaign: [
			{ campaign: "Restaurantes Maianga", usd: 3.1 },
			{ campaign: "Escolas Viana", usd: 2.45 },
			{ campaign: "Oficinas Zango", usd: 1.35 },
		],
		events: [
			{
				at: "2026-09-28T10:12:00+01:00",
				campaign: "Restaurantes Maianga",
				source: "APIFY",
				detail: "Scrape de 410 lugares",
				costUsd: 1.64,
			},
			{
				at: "2026-09-21T15:48:00+01:00",
				campaign: "Escolas Viana",
				source: "LLM",
				detail: "Análise de reviews, 60 leads",
				costUsd: 0.92,
			},
			{
				at: "2026-09-14T09:20:00+01:00",
				campaign: "Oficinas Zango",
				source: "APIFY",
				detail: "Scrape de 205 lugares",
				costUsd: 0.82,
			},
			{
				at: "2026-09-09T17:05:00+01:00",
				campaign: "Escolas Viana",
				source: "APIFY",
				detail: "Scrape de 330 lugares",
				costUsd: 1.32,
			},
		],
	},
	{
		month: "2026-08",
		budgetUsd: 10,
		committedUsd: null,
		apifyUsd: 1.8,
		llmUsd: 0.35,
		byCampaign: [
			{ campaign: "Escolas Viana", usd: 1.3 },
			{ campaign: "Oficinas Zango", usd: 0.85 },
		],
		events: [
			{
				at: "2026-08-26T11:40:00+01:00",
				campaign: "Escolas Viana",
				source: "APIFY",
				detail: "Scrape de 250 lugares",
				costUsd: 1.0,
			},
			{
				at: "2026-08-19T16:10:00+01:00",
				campaign: "Oficinas Zango",
				source: "LLM",
				detail: "Auditoria de 18 websites",
				costUsd: 0.35,
			},
			{
				at: "2026-08-12T10:00:00+01:00",
				campaign: "Oficinas Zango",
				source: "APIFY",
				detail: "Scrape de 200 lugares",
				costUsd: 0.8,
			},
		],
	},
];
