# 0016. Test SQL against real Postgres; fake the scraper

- Date: 2026-09-28
- Status: Accepted

## Context

Most of the logic that can go wrong sits in SQL: upserts, the stage 1 cut with window functions, rules that protect leads the owner already contacted. An in-memory database would not run that SQL the same way. The Apify API costs money and is not reachable from every environment.

## Decision

- Unit tests cover phone parsing, website classification, exclusions, scoring, campaign parsing, and the Apify item mapper. The mapper reads a JSON fixture in `src/test/resources/apify/`.
- The Apify client is tested with Spring's `MockRestServiceServer`, checking the exact endpoints and input fields.
- Integration tests extend `PostgresTestSupport` and run the real pipeline against Postgres with a `FakeScraper`. Testcontainers starts `postgres:17` when Docker is available. Without Docker, the `LEADHUNTER_TEST_JDBC_URL` environment variable points at an existing empty database. With neither, the integration tests are skipped, not failed.
- No test calls the real Apify or Anthropic APIs.

## Consequences

`./mvnw test` works on a laptop with Docker and in restricted environments with a local Postgres. A skipped integration test can hide a broken query, so run them with a database before pushing changes to SQL.
