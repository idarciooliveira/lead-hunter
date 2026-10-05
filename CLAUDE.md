# Lead Hunter

Internal CLI that finds and ranks PME leads from Google Maps for a software factory in Luanda. Java 21, Spring Boot 4.1, Maven, picocli, PostgreSQL with Flyway and JdbcClient, Apify for scraping, LLM calls through the Vercel AI Gateway behind `LlmClient`. See README.md.

## Decisions

Every decision lives in `docs/adr/`. Read `docs/adr/README.md` before changing architecture, libraries, data sources, scoring, or scope.

- A change that picks a library, data source, storage model, scoring approach, or product scope needs a new ADR in the same change. Copy `docs/adr/template.md`, take the next number, and add it to the index in `docs/adr/README.md`.
- Never rewrite an accepted ADR to change the decision. Write a new one and mark the old one `Superseded by NNNN`.
- Scoring weights live in `Stage1Scorer` and are documented in ADR 0007. Change both together.

## Commands

Environment: building needs JDK 21 (a JRE is not enough). It is installed with SDKMAN and pinned in `.sdkmanrc`. Agent and IDE shells do not load SDKMAN, so start every build or test command with `source ~/.sdkman/bin/sdkman-init.sh && sdk env`, or set `JAVA_HOME=~/.sdkman/candidates/java/21.0.8-tem`. `./lh` does this itself. `mvnw` and `lh` must stay LF (`.gitattributes` enforces it); if one fails with `\r` errors, run `git add --renormalize .` and re-checkout the file. If you build in a Docker container, pass `--user $(id -u):$(id -g)`; otherwise `backend/target` ends up root-owned and host builds fail with "Error while storing the mojo status" until it is removed with `sudo rm -rf backend/target`. Postgres comes from `docker compose up -d postgres`, and Docker must be running for the integration tests.

- Build: `cd backend && ./mvnw package -DskipTests`
- Test: `cd backend && ./mvnw test`. Integration tests need Docker, or `LEADHUNTER_TEST_JDBC_URL` pointing at an empty Postgres database.
- Run: `java -jar backend/target/lead-hunter.jar --help` (or `./lh --help`)

## Conventions

- Package by feature under `me.iofdev.leadhunter`: `company`, `campaign`, `maps`, `apify`, `place`, `scoring`, `pipeline`, `llm`, `cli`, `input`.
- SQL is hand-written with `JdbcClient`. Schema changes are new Flyway migrations; never edit an applied one.
- Jackson 3: packages are `tools.jackson.*`, and `JsonNode.asString()` replaces `asText()`.
- CLI output goes through picocli's `spec.commandLine().getOut()` so tests can capture it. Errors are thrown and printed as `error: <message>` with exit code 1.
- No test calls real external APIs. Use `FakeScraper` or `MockRestServiceServer`.
- Code that needs an LLM depends on `LlmClient`, never on a provider SDK.
- Secrets only come from environment variables.
- Commits: Conventional Commits, one line, `type(scope): summary`. The scope is optional. Types are `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `build` and `ci`. The summary is lowercase, imperative and has no trailing period, for example `feat(usage): add the usage command`. Authored by Idarcio Oliveira <idarciooliveira@gmail.com>. No body, no Co-Authored-By, no Claude or session trailers.
- Pull requests: the title follows the same `type(scope): summary` format. The description carries no Claude or session attribution.
