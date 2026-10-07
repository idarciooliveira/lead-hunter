import { Client } from "pg";
import { E2E_DATABASE_URL, E2E_SCHEMA } from "./auth-fixture";

/** Drops the schema `seed-auth.mjs` built. */
export default async function globalTeardown() {
	const client = new Client({ connectionString: E2E_DATABASE_URL });
	await client.connect();
	await client.query(`drop schema if exists ${E2E_SCHEMA} cascade`);
	await client.end();
}
