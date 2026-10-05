/**
 * Stands in for the HTTP API (ADR 0031) until it exists. Every feature's `api.ts`
 * reads its fixtures through here and validates them with the same Zod schema the
 * real responses will go through, so swapping in `fetch("/api/...")` later only
 * touches the `api.ts` files.
 */
import type { z } from "zod";

export async function fakeResponse<T extends z.ZodType>(schema: T, data: unknown): Promise<z.infer<T>> {
	return schema.parse(structuredClone(data));
}

export class NotFoundError extends Error {}
