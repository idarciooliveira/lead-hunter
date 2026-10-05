# 0030. Build the web client with TanStack Start in the same repository

- Date: 2026-10-04
- Status: Proposed
- Amends: [0009](0009-cli-first.md)

## Context

ADR 0009 deferred the web UI until the leads prove themselves. The pipeline, scoring and stage-2 enrichment now work, and reading lead cards in a terminal is the main friction. ADR 0004 still holds: version 1 stops at the ranked list and outreach stays manual. The UI is for one technical owner, then a small team. It is an internal tool with no public signup.

## Decision

- The repository has two top-level apps. The Java code moves from the root into `backend/` (`pom.xml`, `src/`, `mvnw`, `Dockerfile`), and a TanStack Start app lives in `web/` with its own `package.json` and `pnpm` as the package manager. `docs/`, `campaigns/`, `evals/` and `docker-compose.yml` stay at the root.
- The CLI and the API are not separate projects. They stay as the `cli` and `api` packages inside `backend/`, one Maven project and one application with two modes (ADR 0009, 0031). If those packages start reaching into each other, we split `backend/` into Maven modules (`core`, `cli`, `api`) in a new ADR.
- Inside `web/src`: `routes/` for file-based routes, `features/` with one folder per feature (`leads`, `campaigns`, `runs`, `usage`, `company`), `lib/` for the API client and formatters, `components/` for shared UI.
- TanStack Router, Query and Table for routing, data and lists. Tailwind with shadcn/ui for the interface. Zod validates API responses. Vitest for unit tests and Playwright for a few smoke tests.
- The UI shows scores and reasons from the API. It never reimplements scoring, which stays in `Stage1Scorer` and ADR 0007.
- The CLI stays. Both are thin adapters over the same domain code.
- `docker-compose.yml` gets a `web` service for local development.

## Consequences

One repository means one pull request can change the API and the screen that uses it. Two toolchains (Maven and pnpm) need separate CI jobs. Moving the Java code into `backend/` changes build paths in the `Dockerfile`, `docker-compose.yml`, the `lh` script, `.gitignore`, `CLAUDE.md` and the README, and is done as its own commit. Contributors need Node as well as Java 21. Scope is limited to viewing and working the ranked list, editing campaigns and the company profile, starting runs, and reading costs. A CRM or an outreach sender is out of scope, per ADR 0004.
