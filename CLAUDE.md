# Lead Hunter

Internal CLI that finds and ranks PME leads from Google Maps for a software factory in Luanda. Java 21, Spring Boot 4.1, Maven, picocli, PostgreSQL with Flyway and JdbcClient, Apify for scraping, LLM calls through the Vercel AI Gateway behind `LlmClient`. The web client in `web/` is TanStack Start with Query, Router and Table, Tailwind, Zod and Biome. It reads the HTTP API when `VITE_LEADHUNTER_API_URL` is set and sample data otherwise (ADR 0030, 0031, 0035). See README.md and web/README.md.

## Decisions

Every decision lives in `docs/adr/`. Read `docs/adr/README.md` before changing architecture, libraries, data sources, scoring, or scope.

- A change that picks a library, data source, storage model, scoring approach, or product scope needs a new ADR in the same change. Copy `docs/adr/template.md`, take the next number, and add it to the index in `docs/adr/README.md`.
- Never rewrite an accepted ADR to change the decision. Write a new one and mark the old one `Superseded by NNNN`.
- Scoring weights live in `Stage1Scorer` and are documented in ADR 0007. Change both together.

## Commands

Environment: building needs JDK 21 (a JRE is not enough). It is installed with SDKMAN and pinned in `.sdkmanrc`. Agent and IDE shells do not load SDKMAN, so start every build or test command with `source ~/.sdkman/bin/sdkman-init.sh && sdk env`, or set `JAVA_HOME=~/.sdkman/candidates/java/21.0.8-tem`. `./lh` does this itself. `mvnw` and `lh` must stay LF (`.gitattributes` enforces it); if one fails with `\r` errors, run `git add --renormalize .` and re-checkout the file. If you build in a Docker container, pass `--user $(id -u):$(id -g)`; otherwise `backend/target` ends up root-owned and host builds fail with "Error while storing the mojo status" until it is removed with `sudo rm -rf backend/target`. Postgres comes from `docker compose up -d postgres`, and Docker must be running for the integration tests.

- Definition of done: `./check` from the repo root. It checks the JDK and line endings, then runs the build and every test with the integration tests required (ADR 0034), then the web lint, typecheck, unit tests, build and Playwright smoke tests (ADR 0035). `./check backend` or `./check web` runs one half; finish with the half you touched, or both if you touched both. Report its result, not "tests pass".
- Build: `cd backend && ./mvnw package -DskipTests`
- Test: `cd backend && ./mvnw test`. Integration tests need Docker, or `LEADHUNTER_TEST_JDBC_URL` pointing at an empty Postgres database.
- Run: `java -jar backend/target/lead-hunter.jar --help` (or `./lh --help`)
- Web: needs Node 22+ and pnpm (`corepack enable`), plus `pnpm exec playwright install chromium` once for the smoke tests. `cd web && pnpm install && pnpm dev`. After a visual change, `pnpm screenshots` refreshes `docs/screenshots/web`.

## Working rules

- A change is done when `./check` is green and, if it needs one, the ADR is written. Delete code that the change leaves unused.
- A failing test is never "pre-existing". Fix its cause, or stop and report it with the output.
- Reuse before writing. Shared helpers: `Money.usd` and `Format` (formatting), `YamlInput` (YAML reading and validation errors), `CliFiles` and `Prompter` (CLI file and stdin handling), `ApifyRuns` (Apify actor runs), `RunFailure` and `RunRepository` (recording run outcomes). In `web/`: `lib/format.ts` (money, dates, WhatsApp links), `components/ui` (the design system) and `DataTable` for every list. Search the package before adding a second version of one of these.
- Several agents may work at once. Use one branch and one git worktree per agent, keep pull requests small, and before adding a Flyway migration or an ADR check the next free number on `main` and in open pull requests.
- If a doc, an agent file under `.opencode/`, or a README contradicts this file, fix it in the same change.

## Conventions

- Web: one folder per feature under `web/src/features` with `schema.ts` (Zod), `api.ts`, `queries.ts` and `components/`. Pages read data only through the query options; only `api.ts` knows where data comes from. Build screens from `web/src/components/ui` and the token classes in `web/src/styles.css` (`bg-panel`, `text-mute`, `bg-acc`...), never raw colours. The UI shows scores and reasons from the API and never computes them.
- Package by feature under `me.iofdev.leadhunter`: `company`, `campaign`, `maps`, `apify`, `place`, `scoring`, `pipeline`, `llm`, `cli`, `input`.
- SQL is hand-written with `JdbcClient`. Schema changes are new Flyway migrations; never edit an applied one.
- Jackson 3: packages are `tools.jackson.*`, and `JsonNode.asString()` replaces `asText()`.
- CLI output goes through picocli's `spec.commandLine().getOut()` so tests can capture it. Errors are thrown and printed as `error: <message>` with exit code 1.
- No test calls real external APIs. Use `FakeScraper` or `MockRestServiceServer`.
- Code that needs an LLM depends on `LlmClient`, never on a provider SDK.
- Secrets only come from environment variables.
- Commits: Conventional Commits, one line, `type(scope): summary`. The scope is optional. Types are `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `build` and `ci`. The summary is lowercase, imperative and has no trailing period, for example `feat(usage): add the usage command`. Authored by Idarcio Oliveira <idarciooliveira@gmail.com>. No body, no Co-Authored-By, no Claude or session trailers.
- Pull requests: the title follows the same `type(scope): summary` format. The description carries no Claude or session attribution.
