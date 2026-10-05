import { Link } from "@tanstack/react-router";
import { NAV } from "./nav";

/** Bottom tab bar below the `md` breakpoint, where the sidebar is hidden. */
export function MobileNav() {
	return (
		<nav
			aria-label="Principal"
			className="fixed inset-x-0 bottom-0 z-40 flex border-t border-line bg-panel pb-[env(safe-area-inset-bottom)] md:hidden"
		>
			{NAV.map((item) => (
				<Link
					key={item.to}
					to={item.to}
					className="flex h-14 flex-1 flex-col items-center justify-center text-[11px] font-medium text-mute"
					activeProps={{ className: "text-acc-tx" }}
				>
					<span className="text-[15px]">{item.label === "Uso e custos" ? "Uso" : item.label}</span>
					<span>{item.short}</span>
				</Link>
			))}
		</nav>
	);
}
