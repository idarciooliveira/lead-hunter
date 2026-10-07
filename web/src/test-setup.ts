import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";

afterEach(cleanup);

// jsdom lacks these two browser APIs, and cmdk calls them.
globalThis.ResizeObserver ??= class {
	observe() {}
	unobserve() {}
	disconnect() {}
};
Element.prototype.scrollIntoView ??= () => {};

// Every backend call carries the signed-in user (ADR 0043), and a unit test has no request to read a session from.
vi.mock("#/lib/session.server", async (importOriginal) => ({
	...(await importOriginal<typeof import("#/lib/session.server")>()),
	requireViewer: async () => ({
		userId: "user-1",
		name: "Ana",
		email: "ana@example.com",
		orgId: "org-1",
		orgName: "Acme",
	}),
}));
