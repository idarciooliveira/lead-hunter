# Lead Hunter

Finds small companies on Google Maps that need a website, an app, or a system, and ranks them for outreach by WhatsApp or phone. It's built for one user: a software factory looking for PME clients in Luanda. The decisions behind it are in [docs/adr](docs/adr/README.md).

## Status

| Step | What | State |
|---|---|---|
| 1 | Scaffold, schema, campaigns from YAML | done |
| 2 | Apify scraping, stage 1 filters, scoring and cut | done |
| 3 | Website crawl and stage 2 scoring | next |
| 4 | Review analysis and pitches through the Vercel AI Gateway | planned |
| 5 | `today` queue, `lead mark` outcomes, CSV export | planned |
| 6 | Calibration on existing clients | planned |

## How it works

1. A campaign answers the 10 onboarding questions and lists search terms and locations. Create it with `campaign new`, or write the YAML yourself.
2. `campaign run` starts one Apify Google Maps run per location, with no reviews or images to keep it cheap.
3. Each place gets hard filters first: closed, no phone, banks, government, telecoms, big chains, your own clients.
4. The rest get a stage 1 score from rules in `Stage1Scorer`. Every point comes with a reason.
5. The top share of the campaign, 40% by default, becomes `QUALIFIED`. The rest is `BELOW_CUT`, with the reason stored.

Places are shared across campaigns and deduplicated by Google place ID. Re-running a campaign never touches a lead you already worked on.

## Run it locally

First time, in the project root:

```bash
cp .env.example .env          # then replace APIFY_TOKEN and AI_GATEWAY_API_KEY
docker compose up -d postgres # Postgres on localhost:5432, data kept in a Docker volume
```

There are two ways to run commands. Both use the same database, so you can mix them.

### Option A: Java on your machine

Needs Java 21. `./lh` builds the jar the first time, then runs it.

```bash
./lh --help
./lh campaign new                                   # answer the questions, no file needed
./lh campaign create -f campaigns/clinicas-luanda.yml  # or load a ready file
./lh campaign run clinicas-luanda --dry-run
./lh campaign run clinicas-luanda
./lh leads list clinicas-luanda
./lh leads show 12
./lh llm test
```

After changing code, rebuild with `./mvnw package -DskipTests`. `./lh` only builds when the jar is missing. On Windows, use `mvnw.cmd package -DskipTests` and then `java -jar target\lead-hunter.jar <command>`.

### Option B: everything in Docker

Needs only Docker. The first run builds the image, which takes a few minutes.

```bash
docker compose run --rm app --help
docker compose run --rm app campaign new
docker compose run --rm app campaign create -f campaigns/clinicas-luanda.yml
docker compose run --rm app campaign run clinicas-luanda
docker compose run --rm app leads list clinicas-luanda
```

The `app` service reads `.env`, connects to the `postgres` service by name, and mounts `./campaigns` into the container, so campaign files you edit on your machine are visible inside. After changing code, rebuild with `docker compose build app`.

A shorter alias: `alias lhd='docker compose run --rm app'`, then `lhd leads list clinicas-luanda`.

## Commands

| Command | What it does |
|---|---|
| `campaign new [--dir campaigns] [--no-file]` | Ask the 10 questions, save the campaign, and write `campaigns/<slug>.yml` |
| `campaign template` | Print an example campaign file with the 10 questions |
| `campaign create -f <file>` | Save a campaign. Same slug again updates it |
| `campaign list` | Campaigns and how much each has spent on Apify |
| `campaign run <slug> [--dry-run] [--allow-over-limit]` | Scrape, filter, score, cut |
| `leads list <slug> [--stage QUALIFIED\|BELOW_CUT\|EXCLUDED\|ALL] [--limit 20]` | Ranked leads |
| `leads show <id>` | Lead card with score breakdown and WhatsApp link |
| `llm test ["prompt"]` | Send one prompt to the configured model, print the answer and token usage |

## Configuration

Copy [.env.example](.env.example) to `.env` in the project root and replace the values. The app reads `.env` on startup from the directory you run it in, so there's nothing to export. Real environment variables override `.env`, which is how Railway's settings take over in production. `.env` is in `.gitignore`.

| Variable | Default | Notes |
|---|---|---|
| `APIFY_TOKEN` | none | Required for `campaign run`. From console.apify.com, Settings, API & Integrations |
| `AI_GATEWAY_API_KEY` | none | Vercel AI Gateway key. Check it with `llm test` |
| `LEADHUNTER_LLM_MODEL` | `google/gemma-4-26b-a4b-it` | Any gateway model id. Gemma is for testing, see [ADR 0017](docs/adr/0017-vercel-ai-gateway.md) |
| `LEADHUNTER_LLM_BASE_URL` | `https://ai-gateway.vercel.sh/v1` | Any OpenAI-compatible endpoint |
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
