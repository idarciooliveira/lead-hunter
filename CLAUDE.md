# Lead Hunter

Internal CLI that finds and ranks PME leads from Google Maps for a software factory in Luanda. Java 21, Spring Boot 4.1, Maven, picocli, PostgreSQL with Flyway and JdbcClient, Apify for scraping. See README.md.

## Decisions

Every decision lives in `docs/adr/`. Read `docs/adr/README.md` before changing architecture, libraries, data sources, scoring, or scope.

- A change that picks a library, data source, storage model, scoring approach, or product scope needs a new ADR in the same change. Copy `docs/adr/template.md`, take the next number, and add it to the index in `docs/adr/README.md`.
- Never rewrite an accepted ADR to change the decision. Write a new one and mark the old one `Superseded by NNNN`.
- Scoring weights live in `Stage1Scorer` and are documented in ADR 0007. Change both together.

## Commands

- Build: `./mvnw package -DskipTests`
- Test: `./mvnw test`. Integration tests need Docker, or `LEADHUNTER_TEST_JDBC_URL` pointing at an empty Postgres database.
- Run: `java -jar target/lead-hunter.jar --help`

## Conventions

- Package by feature under `me.iofdev.leadhunter`: `campaign`, `maps`, `apify`, `place`, `scoring`, `pipeline`, `cli`.
- SQL is hand-written with `JdbcClient`. Schema changes are new Flyway migrations; never edit an applied one.
- Jackson 3: packages are `tools.jackson.*`, and `JsonNode.asString()` replaces `asText()`.
- CLI output goes through picocli's `spec.commandLine().getOut()` so tests can capture it. Errors are thrown and printed as `error: <message>` with exit code 1.
- No test calls real external APIs. Use `FakeScraper` or `MockRestServiceServer`.
- Secrets only come from environment variables.
