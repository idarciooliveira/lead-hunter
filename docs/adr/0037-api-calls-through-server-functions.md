# 0037. Call the API only from TanStack Start server functions, with a service token

- Date: 2026-10-05
- Status: Accepted
- Supersedes: the token part of [0032](0032-shared-token-auth-for-the-web-client.md)

## Context

The web client reads the HTTP API (ADR 0031) from each feature's `api.ts`. Today those files `fetch` the Spring backend directly: route loaders do it on the TanStack Start server during server rendering, but client navigation, polling and every mutation do it from the browser. That only works because `ApiCorsConfig` allows any origin and the backend URL sits in `VITE_LEADHUNTER_API_URL`, which is bundled into the client.

The API starts paid Apify and LLM runs and returns lead phone numbers. ADR 0032 wanted a token the browser never sees, sent from server functions, but it was never accepted or built. Logins are decided separately in ADR 0038.

## Decision

- The browser never calls the Spring API. Every call goes through a TanStack Start server function (`createServerFn`), defined in the feature's `api.ts`.
- TanStack Query stays the data layer. Query `queryFn`s and mutation `mutationFn`s call the server functions; query keys, loaders, polling and invalidation do not change. Pages still read data only through `queries.ts`.
- Server function inputs are validated with Zod. Responses keep going through the backend contract in `lib/api-contract.ts` and the feature schemas.
- The backend URL and a service token come from server-only environment variables: `LEADHUNTER_API_URL` and `LEADHUNTER_API_TOKEN`. Nothing API-related keeps the `VITE_` prefix.
- Spring requires `Authorization: Bearer <LEADHUNTER_API_TOKEN>` on every `/api/**` route except `/api/health`, and answers 401 otherwise. The open CORS mapping is removed. Spring listens only where the Start server can reach it.
- Once logins exist (ADR 0038), the server function also sends the signed-in user's id in an `X-LeadHunter-User` header. Spring trusts it only because the request carries the service token, and records it where a write needs an author, such as a lead outcome.
- Without `LEADHUNTER_API_URL` the server functions keep returning the fixtures, so the smoke tests and screenshots run without a backend (ADR 0035).

## Consequences

The backend can stay off the public network, and no secret or backend address reaches the browser. Every request takes an extra hop through the Start server, which is negligible next to Apify and LLM latency. A generic `/api/*` proxy would be less code, but it would forward everything; one server function per call keeps each exposed operation explicit. The Playwright integration project must set the server-only variables and the mock API must check the token. Tests that stub `fetch` in the browser move to stubbing the server function's backend call.
