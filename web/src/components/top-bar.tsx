import { Search } from "lucide-react";
import { Kbd } from "#/components/ui/kbd";
import { BudgetPill } from "./budget-pill";
import { ThemeToggle } from "./theme-toggle";

export function TopBar({ onSearch }: { onSearch: () => void }) {
	return (
		<header className="hidden flex-wrap items-center gap-3 border-b border-line bg-panel px-8 py-3 md:flex">
			<button
				type="button"
				onClick={onSearch}
				className="flex h-8 w-[360px] max-w-full cursor-pointer items-center gap-2 rounded-md border border-line2 bg-bg px-2.5 text-mute"
			>
				<Search className="size-3.5" aria-hidden />
				<span className="flex-1 text-left">Pesquisar leads, campanhas, acções</span>
				<Kbd>⌘K</Kbd>
			</button>
			<div className="flex-1" />
			<BudgetPill />
			<ThemeToggle />
		</header>
	);
}
