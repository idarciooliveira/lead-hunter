import { notFound } from "@tanstack/react-router";
import { NotFoundError } from "./fake-api";

/**
 * Runs a read inside a server function (ADR 0037). A missing record becomes
 * the router's `notFound()`, which crosses the server function boundary, so
 * routes show their not-found page. Other errors keep the backend's message.
 */
export async function orNotFound<T>(read: () => Promise<T>): Promise<T> {
	try {
		return await read();
	} catch (e) {
		if (e instanceof NotFoundError) throw notFound();
		throw e;
	}
}
