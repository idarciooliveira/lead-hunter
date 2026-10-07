import { describe, expect, it } from "vitest";
import { gate } from "./session-middleware";

describe("gate", () => {
	it("lets Better Auth's endpoints through, signed in or not", () => {
		expect(gate("/api/auth/sign-in/email", false)).toBe("pass");
		expect(gate("/api/auth/get-session", true)).toBe("pass");
	});

	it("shows the login page to visitors and sends signed-in people home", () => {
		expect(gate("/entrar", false)).toBe("pass");
		expect(gate("/entrar", true)).toBe("home");
	});

	it("sends visitors on a page to the login page", () => {
		expect(gate("/hoje", false)).toBe("login");
		expect(gate("/leads/12", false)).toBe("login");
		expect(gate("/hoje", true)).toBe("pass");
	});

	it("answers server function calls without a session with 401, not a redirect", () => {
		expect(gate("/_serverFn/abc123", false)).toBe("unauthorized");
		expect(gate("/_serverFn/abc123", true)).toBe("pass");
	});
});
