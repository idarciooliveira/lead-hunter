# Architecture decision records

How we record decisions is described in [0001](0001-record-architecture-decisions.md). Copy [template.md](template.md) for a new one and take the next number.

| # | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-internal-tool-first.md) | Build an internal tool for our own lead generation first | Partly superseded by 0043 |
| [0003](0003-target-market.md) | Target small PMEs in Luanda first, then Lubango and Benguela | Accepted |
| [0004](0004-v1-stops-at-the-list.md) | Version 1 stops at the ranked list; outreach stays manual | Accepted |
| [0005](0005-apify-google-maps-scraper.md) | Get Google Maps data through the Apify scraper | Accepted |
| [0006](0006-two-stage-pipeline.md) | Run a two-stage pipeline to stay within a $10 monthly budget | Accepted |
| [0007](0007-rule-based-scoring.md) | Score with explicit rules; the LLM only reads reviews and writes pitches | Accepted |
| [0008](0008-java-spring-boot-maven.md) | Use Java 21, Spring Boot 4, and Maven | Accepted |
| [0009](0009-cli-first.md) | Build a CLI first with picocli; web UI later | Accepted |
| [0010](0010-postgres-flyway-jdbcclient.md) | PostgreSQL with Flyway and JdbcClient; no job queue in v1 | Accepted |
| [0011](0011-railway-and-docker-compose.md) | Deploy on Railway; run locally with Docker Compose | Accepted |
| [0012](0012-track-outcomes.md) | Track contact outcomes per lead to calibrate scoring | Accepted |
| [0013](0013-campaigns-as-yaml.md) | Define campaigns as YAML files built on the 10 questions | Superseded by 0029 |
| [0014](0014-claude-haiku.md) | Use Claude Haiku 4.5 for review analysis and pitches | Superseded by 0017 |
| [0015](0015-on-demand-runs.md) | Run campaigns on demand | Accepted |
| [0016](0016-testing-strategy.md) | Test SQL against real Postgres; fake the scraper | Accepted |
| [0017](0017-vercel-ai-gateway.md) | Call LLMs through the Vercel AI Gateway; test with Gemma 4 26B A4B | Accepted |
| [0018](0018-campaign-wizard.md) | Create campaigns with an interactive wizard; YAML stays the stored format | Accepted |
| [0019](0019-company-profile.md) | Split the questions into a company profile and campaign questions | Accepted |
| [0020](0020-not-now-lost-reason.md) | Add "not now" as a reason for a lost lead | Accepted |
| [0021](0021-track-usage-and-costs.md) | Store what Apify and the LLM cost, and report it with `usage` | Accepted |
| [0022](0022-interactive-menu.md) | Open an interactive menu when the tool starts in a terminal | Accepted |
| [0023](0023-fox-banner-in-interactive-menu.md) | Show a pixel fox banner when the interactive menu starts | Superseded by 0024 |
| [0024](0024-shaded-fox-banner.md) | Render a shaded fox illustration in the interactive menu | Superseded by 0025 |
| [0025](0025-pixel-art-fox-banner.md) | Draw the fox banner as hand-placed pixel art | Accepted |
| [0026](0026-soft-clear-the-screen-in-interactive-mode.md) | Soft-clear the screen in interactive mode | Accepted |
| [0027](0027-stage-two-website-crawl.md) | Enrich qualified leads with a website crawl and stage 2 scoring | Accepted |
| [0028](0028-stage-two-no-issue-marker.md) | Mark a clean stage-2 run with a zero-point STAGE2_NO_ISSUES code | Accepted |
| [0029](0029-database-is-the-only-campaign-store.md) | Store campaigns and the company profile only in the database | Accepted |
| [0030](0030-web-client-with-tanstack-start.md) | Build the web client with TanStack Start in the same repository | Accepted |
| [0031](0031-http-api-for-the-web-client.md) | Add a JSON HTTP API to the backend for the web client | Accepted |
| [0032](0032-shared-token-auth-for-the-web-client.md) | Protect the API with a shared token held by the web server | Superseded by 0037, 0038 |
| [0033](0033-runs-from-the-ui.md) | Start runs from the UI as background jobs that the client polls | Accepted |
| [0034](0034-check-script-and-ci-define-done.md) | Make `./check` and CI the definition of done | Accepted |
| [0035](0035-web-scaffold-on-sample-data.md) | Build the web screens on sample data behind a fake API, with the prototype's design tokens | Accepted |
| [0036](0036-job-leases-for-one-run-per-campaign.md) | Hold one job per campaign with a parent row and a Postgres advisory lock, for the CLI and the API alike | Accepted |
| [0037](0037-api-calls-through-server-functions.md) | Call the API only from TanStack Start server functions, with a service token | Accepted |
| [0038](0038-logins-with-better-auth.md) | Sign users in with Better Auth, stored in our Postgres | Partly superseded by 0042 |
| [0039](0039-full-stack-in-docker-compose-and-a-dev-script.md) | Run the API and the web client with one command, in Docker Compose or with `./dev` | Accepted |
| [0040](0040-one-pitch-per-lead-written-at-enrichment.md) | Write one pitch per lead during enrichment, and drop it when it invents a number | Accepted |
| [0041](0041-today-queue-and-csv-export.md) | Serve the today queue from the backend and export leads as CSV | Accepted |
| [0042](0042-passwords-magic-links-and-accounts-from-the-cli.md) | Sign in with a password or a magic link sent through Resend; accounts come from invitations and the CLI | Accepted |
| [0043](0043-organizations-as-tenants.md) | Make the organization the tenant, with an `org_id` column on its tables | Accepted |
| [0044](0044-owner-keys-with-enforced-budgets.md) | Run every organization on the owner's keys, with budgets that refuse runs | Accepted |
