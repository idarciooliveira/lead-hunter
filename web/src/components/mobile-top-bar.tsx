import { Link } from "@tanstack/react-router";
import { Search } from "lucide-react";
import { Button } from "#/components/ui/button";
import { MobileSignOut } from "#/features/session/components/user-menu";
import { BudgetText } from "./budget-pill";
import { FoxLogo } from "./fox-logo";
import { ThemeToggle } from "./theme-toggle";

/** Below `md` the sidebar and search bar collapse into this strip. */
export function MobileTopBar({ onSearch }: { onSearch: () => void }) {
	return (
		<header className="sticky top-0 z-30 flex items-center gap-2.5 border-b border-line bg-panel px-4 py-2 md:hidden">
			<Link
				to="/hoje"
				aria-label="Ir para a página inicial"
				className="flex min-w-0 flex-1 cursor-pointer items-center gap-2.5"
			>
				<FoxLogo size={24} />
				<div className="flex-1">
					<div className="text-base font-semibold tracking-[-0.01em]">Lead Hunter</div>
					<div className="font-mono text-[11px] text-mute">
						<BudgetText />
					</div>
				</div>
			</Link>
			<Button size="touch-icon" onClick={onSearch} aria-label="Pesquisar">
				<Search className="size-4" aria-hidden />
			</Button>
			<ThemeToggle touch />
			<MobileSignOut />
		</header>
	);
}
