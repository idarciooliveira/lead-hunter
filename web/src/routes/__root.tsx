import { TanStackDevtools } from "@tanstack/react-devtools";
import type { QueryClient } from "@tanstack/react-query";
import { createRootRouteWithContext, HeadContent, Outlet, ScriptOnce, Scripts } from "@tanstack/react-router";
import { TanStackRouterDevtoolsPanel } from "@tanstack/react-router-devtools";
import { NotFound } from "#/components/not-found";
import { FoxState } from "#/components/states";
import { TooltipProvider } from "#/components/ui/tooltip";
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
	shellComponent: RootDocument,
	component: Outlet,
	notFoundComponent: NotFound,
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
				{/* SSR-only: applies the stored theme before paint, then removes itself. */}
				<ScriptOnce>{themeScript}</ScriptOnce>
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
