import { createFileRoute, Outlet } from "@tanstack/react-router";
import { AppShell } from "#/components/app-shell";
import { NotFound } from "#/components/not-found";
import { companyQuery } from "#/features/company/queries";
import { viewerQuery } from "#/features/session/queries";
import { usageQuery } from "#/features/usage/queries";

/** Every page behind the login. The login page lives outside it, because these loaders need a session. */
export const Route = createFileRoute("/_app")({
	// The shell shows the company name, this month's budget and who is signed in on every page.
	loader: ({ context }) =>
		Promise.all([
			context.queryClient.ensureQueryData(companyQuery()),
			context.queryClient.ensureQueryData(usageQuery()),
			context.queryClient.ensureQueryData(viewerQuery()),
		]),
	component: () => (
		<AppShell>
			<Outlet />
		</AppShell>
	),
	notFoundComponent: NotFound,
});
