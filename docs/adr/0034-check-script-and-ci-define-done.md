# 0034. Make `./check` and CI the definition of done

- Date: 2026-10-05
- Status: Accepted

## Context

Several agents and people now work in this repository, and more will. Agents reported "tests pass" while the integration tests were skipped, because ADR 0016 skips them when no database is reachable. A QA round passed with two failing tests that it called pre-existing. Builds broke on a missing JDK, CRLF line endings in `mvnw`, and root-owned `target/` directories. There was no CI.

## Decision

- `./check` is the definition of done. It checks for JDK 21 and LF line endings, requires Docker or `LEADHUNTER_TEST_JDBC_URL`, then runs `mvnw verify` with `LEADHUNTER_REQUIRE_DB=true`.
- With `LEADHUNTER_REQUIRE_DB=true`, `PostgresTestSupport` fails when no database is available instead of skipping. Without it, ADR 0016 still applies, so a plain `./mvnw test` keeps working on a laptop without Docker.
- GitHub Actions runs `./check` on every pull request and every push to `main`.
- A failing test is never described as pre-existing. It is fixed, or reported as a blocker.

## Consequences

An agent has one command to run and one result to report. A skipped integration test can no longer pass for a green build in CI or in `./check`. `./check` needs Docker, so it is slower than `./mvnw test` and not suited to every edit; use it before finishing a change. The `web/` app will add its own steps to `./check` and to the CI workflow when it lands.
