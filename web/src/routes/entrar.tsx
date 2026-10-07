import { createFileRoute } from "@tanstack/react-router";
import { z } from "zod";
import { LoginForm } from "#/features/session/components/login-form";

/** The one page outside the shell. The session middleware sends signed-out visitors here with `next` set. */
export const Route = createFileRoute("/entrar")({
	validateSearch: z.object({ next: z.string().optional() }),
	component: () => <LoginForm next={Route.useSearch().next} />,
});
