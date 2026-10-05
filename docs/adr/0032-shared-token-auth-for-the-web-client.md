# 0032. Protect the API with a shared token held by the web server

- Date: 2026-10-04
- Status: Proposed

## Context

The tool is internal, with one owner and later a small team. The API can start paid Apify and LLM runs and exposes lead phone numbers. Secrets only come from environment variables. The browser must never see the Apify or LLM keys.

## Decision

- The API requires a bearer token read from an environment variable, `LEADHUNTER_API_TOKEN`. Requests without it get 401. `/api/health` is open.
- The token is held by the TanStack Start server and sent from server functions. The browser never receives it.
- Access to the web app itself uses a single shared login checked by the TanStack Start server, with a signed session cookie. Per-user accounts are not built.
- Per-user logins are revisited when more than one person works leads. That would need a new ADR and a `user` table.

## Consequences

Simple to build and to rotate. There is no audit trail of who changed a lead, so outcome tracking (ADR 0012) cannot say who made a call. The shared login is only as strong as its password, so the deployment must use HTTPS.
