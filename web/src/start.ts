import { createCsrfMiddleware, createStart } from "@tanstack/react-start";
import { sessionMiddleware } from "#/lib/session-middleware";

// Server functions are same-origin RPC, so reject cross-site calls before the session check.
const csrfMiddleware = createCsrfMiddleware({
	filter: (ctx) => ctx.handlerType === "serverFn",
});

export const startInstance = createStart(() => ({ requestMiddleware: [csrfMiddleware, sessionMiddleware] }));
