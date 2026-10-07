import { useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { LogOut } from "lucide-react";
import { Button } from "#/components/ui/button";
import { authClient } from "#/lib/auth-client";
import { viewerQuery } from "../queries";

/** Ends the session, then empties the query cache so the next person on this browser never sees this one's data. */
function useSignOut() {
	const navigate = useNavigate();
	const queryClient = useQueryClient();
	return async () => {
		await authClient.signOut();
		queryClient.clear();
		await navigate({ to: "/entrar", search: { next: undefined } });
	};
}

/** Name, organization and sign-out at the foot of the sidebar. Shows nothing on fixtures, which have no login. */
export function UserMenu() {
	const { data: viewer } = useSuspenseQuery(viewerQuery());
	const signOut = useSignOut();
	if (!viewer) return null;
	return (
		<div className="mt-2 flex items-center gap-2 border-t border-line px-2 pt-3">
			<div className="min-w-0 flex-1">
				<div className="truncate font-medium">{viewer.name}</div>
				<div className="truncate text-[11px] text-mute">{viewer.orgName}</div>
			</div>
			<Button size="icon-sm" variant="ghost" onClick={signOut} aria-label="Sair" title="Sair">
				<LogOut className="size-3.5" aria-hidden />
			</Button>
		</div>
	);
}

/** Sign-out for the mobile top bar, where the sidebar is hidden. */
export function MobileSignOut() {
	const { data: viewer } = useSuspenseQuery(viewerQuery());
	const signOut = useSignOut();
	if (!viewer) return null;
	return (
		<Button size="touch-icon" onClick={signOut} aria-label="Sair">
			<LogOut className="size-4" aria-hidden />
		</Button>
	);
}
