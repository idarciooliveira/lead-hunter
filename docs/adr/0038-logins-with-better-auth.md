# 0038. Sign users in with Better Auth, stored in our Postgres

- Date: 2026-10-05
- Status: Accepted
- Supersedes: the shared-login part of [0032](0032-shared-token-auth-for-the-web-client.md)

## Context

The web app needs a login before it leaves the owner's machine. ADR 0032 proposed one shared password with a signed session cookie, and noted the cost: nobody can tell who made a call or marked a lead (ADR 0012). The tool is internal, with one owner now and a small team later, and it already runs Postgres (ADR 0010). Secrets only come from environment variables.

We compared Better Auth, an open-source library that runs inside the TanStack Start server and stores users and sessions in our database, with Clerk, a hosted service with its own user store, login UI and per-user pricing past a free tier.

## Decision

- Logins use Better Auth, running in the TanStack Start server. Each person has their own account; email and password to start.
- Users, sessions and accounts live in our Postgres. Their tables are created by a Flyway migration generated once from Better Auth's schema, so Flyway stays the only thing that changes the schema (ADR 0010). Better Auth's own migrator is not run.
- Sessions are httpOnly cookies on the web app's origin. Every server function (ADR 0037) checks the session first and rejects calls without one.
- There is no public sign-up. The owner creates accounts. Social login, MFA and roles wait until there is a need, and each would be a new ADR.
- The auth secret comes from an environment variable, `BETTER_AUTH_SECRET`.
- This is built after the web reaches feature parity with the CLI. Until then the app runs only on a trusted network.

## Consequences

No vendor bill, no external service in the login path, and user ids are plain rows the backend can reference, so lead outcomes can record who marked them. We own the login page and its styling, password resets and keeping the library updated, which Clerk would have done for us. Moving to Clerk later means migrating users and replacing the session check in the server functions; the rest of the app does not see which provider is used.
