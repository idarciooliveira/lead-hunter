import { afterEach, describe, expect, it, vi } from "vitest";
import { apiBaseUrl } from "./api-config.server";

afterEach(() => vi.unstubAllEnvs());

describe("apiBaseUrl", () => {
	it("is null without configuration, so pages read fixtures", () => {
		vi.stubEnv("LEADHUNTER_API_URL", "");
		expect(apiBaseUrl()).toBeNull();
	});

	it("trims the origin and drops trailing slashes", () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://localhost:8080///");
		expect(apiBaseUrl()).toBe("http://localhost:8080");
	});

	it("keeps the /api suffix and drops trailing slashes", () => {
		vi.stubEnv("LEADHUNTER_API_URL", "http://localhost:8080/api///");
		expect(apiBaseUrl()).toBe("http://localhost:8080/api");
	});
});
