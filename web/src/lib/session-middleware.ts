import { createMiddleware } from "@tanstack/react-start";

const LOGIN_PATH = "/entrar";
const HOME_PATH = "/hoje";
const AUTH_PREFIX = "/api/auth/";
const SERVER_FN_PREFIX = "/_serverFn/";

/** What the session middleware does with a request. Pure, so a test covers every case without a server. */
export type Gate = "pass" | "home" | "login" | "unauthorized";

export function gate(path: string, signedIn: boolean): Gate {
	if (path.startsWith(AUTH_PREFIX)) return "pass";
	if (path === LOGIN_PATH) return signedIn ? "home" : "pass";
	if (signedIn) return "pass";
	return path.startsWith(SERVER_FN_PREFIX) ? "unauthorized" : "login";
}

function redirectTo(location: string): Response {
	return new Response(null, { status: 302, headers: { location } });
}

/**
 * Runs before every request (`start.ts`), so a route or server function added later cannot forget the check.
 * Pages send visitors to the login page, server function calls get a 401. Without an API there is no login (ADR 0042).
 * This file is imported by the client entry, so the server-only modules load inside the handler, which the
 * Start compiler strips from the client bundle.
 */
export const sessionMiddleware = createMiddleware({ type: "request" }).server(async ({ request, next }) => {
	const { authEnabled } = await import("./api-config.server");
	if (!authEnabled()) return next();
	const { viewerOf } = await import("./session.server");
	const url = new URL(request.url);
	const signedIn = url.pathname.startsWith(AUTH_PREFIX) ? false : (await viewerOf(request)) !== null;
	switch (gate(url.pathname, signedIn)) {
		case "pass":
			return next();
		case "home":
			return redirectTo(HOME_PATH);
		case "unauthorized":
			return new Response("not signed in", { status: 401 });
		case "login":
			return redirectTo(`${LOGIN_PATH}?next=${encodeURIComponent(url.pathname + url.search)}`);
	}
});
