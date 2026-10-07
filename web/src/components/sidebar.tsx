import { Link } from "@tanstack/react-router";
import { PlannedChip } from "#/components/ui/chip";
import { Kbd } from "#/components/ui/kbd";
import { UserMenu } from "#/features/session/components/user-menu";
import { FoxLogo } from "./fox-logo";
import { NAV, STATES_NAV } from "./nav";

const navItem =
	"flex h-8 w-full items-center justify-between rounded-md px-2.5 font-medium text-mute hover:bg-soft hover:text-tx";
const navActive = { className: "bg-acc-soft text-acc-tx hover:bg-acc-soft hover:text-acc-tx" };

export function Sidebar({ companyName }: { companyName: string }) {
	return (
		<aside className="hidden w-[232px] flex-none border-r border-line bg-panel md:block">
			<div className="sticky top-0 flex h-screen flex-col gap-1 px-3 py-4">
				<Link
					to="/hoje"
					aria-label="Ir para a página inicial"
					className="flex cursor-pointer items-center gap-2.5 rounded-md px-2 pt-1 pb-4"
				>
					<FoxLogo />
					<div>
						<div className="text-base font-semibold tracking-[-0.01em]">Lead Hunter</div>
						<div className="text-[11px] text-mute">{companyName}</div>
					</div>
				</Link>
				<nav aria-label="Principal" className="flex flex-col gap-1">
					{NAV.map((item) => (
						<Link key={item.to} to={item.to} className={navItem} activeProps={navActive}>
							<span>{item.label}</span>
							<Kbd>{item.keys}</Kbd>
						</Link>
					))}
				</nav>
				<div className="flex-1" />
				<Link to={STATES_NAV.to} className={navItem} activeProps={navActive}>
					<span>{STATES_NAV.label}</span>
					<Kbd>{STATES_NAV.keys}</Kbd>
				</Link>
				<div className="mt-2 border-t border-line px-2 pt-3 text-[11px] leading-normal text-mute">
					Dados de exemplo enquanto a API não existe. Ecrãs marcados como <PlannedChip /> ainda não existem no backend.
				</div>
				<UserMenu />
			</div>
		</aside>
	);
}
