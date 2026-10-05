import { useSuspenseQuery } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { type ReactNode, useEffect, useState } from "react";
import { companyQuery } from "#/features/company/queries";
import { useTheme } from "#/lib/theme";
import { useHotkeys } from "#/lib/use-hotkeys";
import { CommandPalette } from "./command-palette";
import { MobileNav } from "./mobile-nav";
import { MobileTopBar } from "./mobile-top-bar";
import { CHORDS } from "./nav";
import { Sidebar } from "./sidebar";
import { TopBar } from "./top-bar";

export function AppShell({ children }: { children: ReactNode }) {
	const [paletteOpen, setPaletteOpen] = useState(false);
	const navigate = useNavigate();
	const { toggle } = useTheme();
	// The root loader has already filled the cache, so this never suspends.
	const { data: company } = useSuspenseQuery(companyQuery());

	useEffect(() => {
		// Lets end-to-end tests wait until keyboard shortcuts are live.
		document.documentElement.dataset.hydrated = "true";
		const onKey = (e: KeyboardEvent) => {
			if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
				e.preventDefault();
				setPaletteOpen((open) => !open);
			}
		};
		window.addEventListener("keydown", onKey);
		return () => window.removeEventListener("keydown", onKey);
	}, []);

	useHotkeys(
		{
			...Object.fromEntries(Object.entries(CHORDS).map(([key, to]) => [`g ${key}`, () => navigate({ to })])),
			n: () => navigate({ to: "/campanhas/nova" }),
			t: toggle,
		},
		!paletteOpen,
	);

	return (
		<div className="flex min-h-screen items-stretch">
			<Sidebar companyName={company.name} />
			<main className="flex min-w-0 flex-1 flex-col">
				<TopBar onSearch={() => setPaletteOpen(true)} />
				<MobileTopBar onSearch={() => setPaletteOpen(true)} />
				<div className="mx-auto w-full max-w-[1344px] px-4 pt-4 pb-24 md:px-8 md:pt-6 md:pb-16">{children}</div>
			</main>
			<MobileNav />
			<CommandPalette open={paletteOpen} onOpenChange={setPaletteOpen} />
		</div>
	);
}
