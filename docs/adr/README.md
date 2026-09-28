# Architecture decision records

How we record decisions is described in [0001](0001-record-architecture-decisions.md). Copy [template.md](template.md) for a new one and take the next number.

| # | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-internal-tool-first.md) | Build an internal tool for our own lead generation first | Accepted |
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
| [0013](0013-campaigns-as-yaml.md) | Define campaigns as YAML files built on the 10 questions | Accepted |
| [0014](0014-claude-haiku.md) | Use Claude Haiku 4.5 for review analysis and pitches | Accepted |
| [0015](0015-on-demand-runs.md) | Run campaigns on demand | Accepted |
| [0016](0016-testing-strategy.md) | Test SQL against real Postgres; fake the scraper | Accepted |
