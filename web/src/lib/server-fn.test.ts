import { isNotFound } from "@tanstack/react-router";
import { describe, expect, it } from "vitest";
import { NotFoundError } from "./fake-api";
import { orNotFound } from "./server-fn";

describe("orNotFound", () => {
	it("turns a missing record into the router's notFound", async () => {
		const error = await orNotFound(() => Promise.reject(new NotFoundError("no lead with id 1"))).catch((e) => e);
		expect(isNotFound(error)).toBe(true);
	});

	it("keeps any other error and its message", async () => {
		await expect(orNotFound(() => Promise.reject(new Error("unknown stage 'X'")))).rejects.toThrow("unknown stage 'X'");
	});
});
