import { createFileRoute, Link } from "@tanstack/react-router";
import type * as React from "react";
import { Page, PageHeader } from "#/components/page-header";
import { FoxState, LoadingState } from "#/components/states";
import { Button } from "#/components/ui/button";
import { Card } from "#/components/ui/card";
import { Cost } from "#/components/ui/cost";

export const Route = createFileRoute("/estados")({
	head: () => ({ meta: [{ title: "Estados do sistema · Lead Hunter" }] }),
	component: StatesPage,
});

function Panel({ tag, children }: { tag: string; children: React.ReactNode }) {
	return (
		<Card className="flex min-h-[300px] flex-col gap-3 p-4">
			<div className="eyebrow font-mono">{tag}</div>
			{children}
		</Card>
	);
}

/** A gallery of the empty, loading and error states the real screens use. */
function StatesPage() {
	return (
		<Page>
			<PageHeader
				title="Estados do sistema"
				subtitle="Vazio, a carregar e erro, como aparecem em campanhas, leads e numa execução Apify falhada."
			/>
			<div className="grid grid-cols-[repeat(auto-fill,minmax(min(380px,100%),1fr))] gap-4">
				<Panel tag="Campanhas · vazio">
					<FoxState
						title="Ainda não há campanhas"
						text="Cria a primeira para procurar negócios no Google Maps."
						action={
							<Button variant="primary" asChild>
								<Link to="/campanhas/nova">Nova campanha</Link>
							</Button>
						}
					/>
				</Panel>
				<Panel tag="Leads · vazio">
					<FoxState
						title="Nenhum lead com estes filtros"
						text="Baixa a pontuação mínima ou limpa os filtros."
						action={
							<Button asChild>
								<Link to="/leads">Limpar filtros</Link>
							</Button>
						}
					/>
				</Panel>
				<Panel tag="Campanhas · a carregar">
					<LoadingState text="A carregar campanhas…" />
				</Panel>
				<Panel tag="Leads · a carregar">
					<LoadingState text="A carregar leads…" />
				</Panel>
				<Panel tag="Execução Apify falhou">
					<FoxState
						title="A execução falhou"
						text="Nada foi cobrado. Podes repetir só os lugares que faltam."
						error="run_0230 · timeout do actor após 15 min"
						action={
							<Button variant="primary" asChild>
								<Link to="/campanhas/$slug" params={{ slug: "clinicas-talatona" }}>
									Tentar de novo <Cost onAccent>≈ $0.84</Cost>
								</Link>
							</Button>
						}
					/>
				</Panel>
				<Panel tag="Sem ligação">
					<FoxState
						title="Os leads não carregaram"
						text="Verifica a ligação e tenta outra vez."
						error="Não foi possível contactar o servidor"
						action={
							<Button asChild>
								<Link to="/leads">Tentar de novo</Link>
							</Button>
						}
					/>
				</Panel>
			</div>
		</Page>
	);
}
