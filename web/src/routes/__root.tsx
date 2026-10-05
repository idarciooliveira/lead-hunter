import { TanStackDevtools } from "@tanstack/react-devtools";
import type { QueryClient } from "@tanstack/react-query";
import { createRootRouteWithContext, HeadContent, Link, Outlet, Scripts } from "@tanstack/react-router";
import { TanStackRouterDevtoolsPanel } from "@tanstack/react-router-devtools";
import { AppShell } from "#/components/app-shell";
import { FoxState } from "#/components/states";
import { Button } from "#/components/ui/button";
import { TooltipProvider } from "#/components/ui/tooltip";
import { companyQuery } from "#/features/company/queries";
import { usageQuery } from "#/features/usage/queries";
import TanStackQueryDevtools from "#/integrations/tanstack-query/devtools";
import { ThemeProvider, themeScript } from "#/lib/theme";
import appCss from "../styles.css?url";

export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
	head: () => ({
		meta: [
			{ charSet: "utf-8" },
			{ name: "viewport", content: "width=device-width, initial-scale=1" },
			{ title: "Lead Hunter" },
		],
		links: [{ rel: "stylesheet", href: appCss }],
	}),
	// The shell shows the company name and this month's budget on every page.
	loader: ({ context }) =>
		Promise.all([
			context.queryClient.ensureQueryData(companyQuery()),
			context.queryClient.ensureQueryData(usageQuery()),
		]),
	shellComponent: RootDocument,
	component: () => (
		<AppShell>
			<Outlet />
		</AppShell>
	),
	notFoundComponent: () => (
		<FoxState
			title="Página não encontrada"
			text="Este endereço não existe no Lead Hunter."
			action={
				<Button asChild variant="primary">
					<Link to="/hoje">Ir para Hoje</Link>
				</Button>
			}
		/>
	),
	errorComponent: ({ error }) => (
		<FoxState
			title="Algo correu mal"
			text="A página não carregou."
			error={error instanceof Error ? error.message : String(error)}
		/>
	),
});

function RootDocument({ children }: { children: React.ReactNode }) {
	return (
		<html lang="pt" suppressHydrationWarning>
			<head>
				{/* biome-ignore lint/security/noDangerouslySetInnerHtml: static script that applies the stored theme before paint. */}
				<script dangerouslySetInnerHTML={{ __html: themeScript }} />
				<HeadContent />
			</head>
			<body>
				<ThemeProvider>
					<TooltipProvider>{children}</TooltipProvider>
				</ThemeProvider>
				{import.meta.env.DEV && (
					<TanStackDevtools
						config={{ position: "bottom-right" }}
						plugins={[{ name: "Tanstack Router", render: <TanStackRouterDevtoolsPanel /> }, TanStackQueryDevtools]}
					/>
				)}
				<Scripts />
			</body>
		</html>
	);
}
