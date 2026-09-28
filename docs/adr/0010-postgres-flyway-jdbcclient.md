# 0010. Store data in PostgreSQL with Flyway and JdbcClient; no job queue in v1

- Date: 2026-09-28
- Status: Accepted

## Context

The pipeline upserts places by Google place ID, stores raw scraper JSON, and stores score breakdowns. The CLI runs one campaign at a time in a single process.

## Decision

- PostgreSQL, the same engine locally and on Railway.
- Flyway migrations in `src/main/resources/db/migration`.
- Spring's `JdbcClient` with hand-written SQL instead of JPA. Upserts with `ON CONFLICT` and `jsonb` columns are simpler in plain SQL than through an ORM.
- No job queue. A campaign run executes inside the CLI process. When the web UI needs background runs, we will add a Postgres-backed queue such as JobRunr and record that in a new ADR.

## Consequences

SQL is visible and easy to tune. We write row mappers by hand. No Redis to pay for on Railway.
