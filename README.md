# Lead Hunter

Finds small companies on Google Maps that need a website, an app, or a system, and ranks them for outreach by WhatsApp or phone. It's built for one user: a software factory looking for PME clients in Luanda. The decisions behind it are in [docs/adr](docs/adr/README.md).

## Status

| Step | What | State |
|---|---|---|
| 1 | Scaffold, schema, campaigns from YAML | done |
| 2 | Apify scraping, stage 1 filters, scoring and cut | done |
| 3 | Website crawl and stage 2 scoring | next |
| 4 | Review analysis and pitches with Claude | planned |
| 5 | `today` queue, `lead mark` outcomes, CSV export | planned |
| 6 | Calibration on existing clients | planned |

## How it works

1. A campaign file answers the 10 onboarding questions and lists search terms and locations.
2. `campaign run` starts one Apify Google Maps run per location, with no reviews or images to keep it cheap.
3. Each place gets hard filters first: closed, no phone, banks, government, telecoms, big chains, your own clients.
4. The rest get a stage 1 score from rules in `Stage1Scorer`. Every point comes with a reason.
5. The top share of the campaign, 40% by default, becomes `QUALIFIED`. The rest is `BELOW_CUT`, with the reason stored.

Places are shared across campaigns and deduplicated by Google place ID. Re-running a campaign never touches a lead you already worked on.

## Run it locally

Requirements: Java 21 and Docker.

```bash
docker compose up -d                     # Postgres on localhost:5432
cp .env.example .env                     # then fill in APIFY_TOKEN
set -a; source .env; set +a
./mvnw package -DskipTests
alias lh='java -jar target/lead-hunter.jar'

lh campaign template > campaigns/escolas-luanda.yml   # edit it
lh campaign create --file campaigns/clinicas-luanda.yml
lh campaign run clinicas-luanda --dry-run             # shows searches and max cost, calls nothing
lh campaign run clinicas-luanda
lh leads list clinicas-luanda
lh leads show 12
```

## Commands

| Command | What it does |
|---|---|
| `campaign template` | Print an example campaign file with the 10 questions |
| `campaign create -f <file>` | Save a campaign. Same slug again updates it |
| `campaign list` | Campaigns and how much each has spent on Apify |
| `campaign run <slug> [--dry-run] [--allow-over-limit]` | Scrape, filter, score, cut |
| `leads list <slug> [--stage QUALIFIED\|BELOW_CUT\|EXCLUDED\|ALL] [--limit 20]` | Ranked leads |
| `leads show <id>` | Lead card with score breakdown and WhatsApp link |

## Configuration

| Variable | Default | Notes |
|---|---|---|
| `APIFY_TOKEN` | none | Required for `campaign run` |
| `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` | docker-compose values | Railway sets these on its Postgres |
| `LEADHUNTER_APIFY_MAX_PLACES_PER_RUN` | 600 | Budget guard. Larger runs need `--allow-over-limit` |
| `LEADHUNTER_APIFY_ESTIMATED_USD_PER_PLACE` | 0.004 | Only for `--dry-run`. Set it from the actor's pricing page |

## Costs

The budget is $10 a month for scraping and LLM calls, see [ADR 0006](docs/adr/0006-two-stage-pipeline.md). Every Apify run stores what it cost in `campaign_run.cost_usd`, and `campaign list` sums it per campaign. Always `--dry-run` a new campaign first.

## Railway

Create a Railway project with a Postgres database and a service built from the `Dockerfile`. The image's default command prints help and exits, so either:

- run the CLI from your laptop against Railway's database: `railway run java -jar target/lead-hunter.jar leads list clinicas-luanda`, or
- set the service's start command to a campaign run and give it a cron schedule, with restart policy "Never".

## Tests

```bash
./mvnw test
```

Integration tests need Postgres. They use Testcontainers when Docker is running. Without Docker, point them at an empty database:

```bash
LEADHUNTER_TEST_JDBC_URL=jdbc:postgresql://localhost:5432/leadhunter_test ./mvnw test
```

See [ADR 0016](docs/adr/0016-testing-strategy.md).
