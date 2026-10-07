import { createFileRoute } from "@tanstack/react-router";
import { getAuth } from "#/lib/auth.server";

/** Better Auth's own endpoints (sign in, sign out, session) on the web origin, so the browser never calls Spring (ADR 0037). */
export const Route = createFileRoute("/api/auth/$")({
	server: {
		handlers: {
			GET: ({ request }) => getAuth().handler(request),
			POST: ({ request }) => getAuth().handler(request),
		},
	},
});
