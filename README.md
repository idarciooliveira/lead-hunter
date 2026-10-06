# Lead Hunter

Finds small companies on Google Maps that need a website, an app, or a system, and ranks them for outreach by WhatsApp or phone. It's built for one user: a software factory looking for PME clients in Luanda. The decisions behind it are in [docs/adr](docs/adr/README.md).

## Status

| Step | What | State |
|---|---|---|
| 1 | Scaffold, schema, campaigns from YAML | done |
| 2 | Apify scraping, stage 1 filters, scoring and cut | done |
| 3 | Website crawl and stage 2 scoring | done |
| 4 | Review analysis and pitches through the Vercel AI Gateway | planned |
| 5 | `today` queue, `lead mark` outcomes, CSV export | planned |
| 6 | Calibration on existing clients | planned |

## How it works

1. The company profile says what you sell, at what price, to whom you've sold, and what you can prove. You answer it once with `company setup`, see [ADR 0019](docs/adr/0019-company-profile.md).
2. A campaign answers 11 questions about its goal: sector, the problem you bet on, the service you pitch, which Maps signals qualify or rule out a place, and the goal with a stop rule. Then it lists search terms and locations. Create it with `campaign new`, or write the YAML yourself.
3. `campaign run` starts one Apify Google Maps run per location, with no reviews or images to keep it cheap.
4. Each place gets hard filters first: closed, no phone, your current clients (by phone, then name), banks, government, telecoms, big chains, fewer reviews than the campaign minimum, and the campaign's disqualifying signals.
5. The rest get a stage 1 score from rules in `Stage1Scorer`. Every point comes with a reason.
6. The top share of the campaign, 40% by default, becomes `QUALIFIED`. The rest is `BELOW_CUT`, with the reason stored.
7. `campaign enrich` crawls each qualified lead's website, fetches its recent reviews, classifies what customers complain about, and adds the stage 2 points to the score. See [ADR 0027](docs/adr/0027-stage-two-website-crawl.md).

Places are shared across campaigns and deduplicated by Google place ID. Re-running a campaign never touches a lead you already worked on.

## Run it locally

First time, in the project root:

```bash
cp .env.example .env          # then replace APIFY_TOKEN and AI_GATEWAY_API_KEY
docker compose up -d postgres # Postgres on localhost:5432, data kept in a Docker volume
```

There are two ways to run commands. Both use the same database, so you can mix them.

### Option A: Java on your machine

Needs a JDK 21 (a JRE is not enough to build; `.sdkmanrc` pins it for SDKMAN, so `sdk env install` sets it up; then `sdk env` in each new shell, or enable `sdkman_auto_env=true` in `sdk config`). `./lh` finds the pinned JDK on its own, but `mvnw` needs `sdk env` or `JAVA_HOME`. Line endings are pinned by `.gitattributes`, so `./lh` and `backend/mvnw` stay LF even with `core.autocrlf=true`. `./lh` builds the jar the first time, then runs it.

```bash
./lh                                                # no arguments in a terminal: the interactive menu
./lh --help
./lh company setup                                  # once: your company, services, clients, cases
./lh campaign new                                   # answer the campaign questions, no file needed
./lh campaign create -f my-campaign.yml            # or load a file (see `campaign template`)
./lh campaign run clinicas-luanda --dry-run
./lh campaign run clinicas-luanda
./lh campaign enrich clinicas-luanda --dry-run
./lh campaign enrich clinicas-luanda
./lh leads list clinicas-luanda
./lh leads show 12
./lh usage
./lh llm test
```

After changing code, rebuild with `./backend/mvnw -f backend/pom.xml package -DskipTests`. `./lh` only builds when the jar is missing. On Windows, use `backend\mvnw.cmd -f backend\pom.xml package -DskipTests` and then `java -jar backend\target\lead-hunter.jar <command>`.

### Option B: everything in Docker

Needs only Docker. The first run builds the image, which takes a few minutes.

```bash
docker compose run --rm app                          # the interactive menu
docker compose run --rm app --help
docker compose run --rm app company setup
docker compose run --rm app campaign new
docker compose run --rm app campaign create -f campaigns/my-campaign.yml
docker compose run --rm app campaign run clinicas-luanda
docker compose run --rm app leads list clinicas-luanda
```

The `app` service reads `.env`, connects to the `postgres` service by name, and mounts `./campaigns` into the container, so YAML files you put there (for `campaign create -f` and `company update -f`) are visible inside. The database holds the campaigns and the company profile; these files are only a way to load them. After changing code, rebuild with `docker compose build app`.

A shorter alias: `alias lhd='docker compose run --rm app'`, then `lhd leads list clinicas-luanda`.

## Commands

| Command | What it does |
|---|---|
| `menu` | Numbered menu to run a campaign, browse leads, create a campaign, see the company profile and usage. Opens by itself when you run with no arguments in a terminal. Greets you with a shaded fox illustration (monochrome text without color; honor `NO_COLOR`) |
| `company setup` | Ask the company questions and save the profile. Run it again to change answers |
| `company update -f <file>` | Save the profile from a YAML file |
| `company show` | Print the saved profile |
| `company template` | Print an example company file |
| `campaign new` | Ask the campaign questions and save the campaign. Needs a company profile |
| `campaign template` | Print an example campaign file |
| `campaign create -f <file>` | Save a campaign. Same slug again updates it. The service must be one the company sells |
| `campaign list` | Campaigns and how much each has spent on Apify |
| `campaign run <slug> [--dry-run] [--allow-over-limit]` | Scrape, filter, score, cut |
| `campaign enrich <slug> [--dry-run] [--batch-size 25] [--max-reviews 10]` | Crawl websites, fetch reviews, classify complaints, rescore the qualified leads |
| `leads list <slug> [--stage QUALIFIED\|BELOW_CUT\|EXCLUDED\|ALL] [--limit 20]` | Ranked leads |
| `leads show <id>` | Lead card with score breakdown and WhatsApp link |
| `leads mark <id> --status <status> [--lost-reason <reason>] [--note <text>]` | Mark a contact outcome. `LOST` needs one of `NO_BUDGET`, `WRONG_PERSON`, `HAS_SUPPLIER`, `NOT_INTERESTED`, `NOT_NOW` |
| `usage [--month YYYY-MM] [--campaign <slug>] [--runs] [--limit 30]` | What Apify and the LLM have cost, with a monthly budget bar and spend per campaign. `--runs` lists each run and call |
| `llm test ["prompt"]` | Send one prompt to the configured model, print the answer, token usage and cost |

## Web client

The browser version lives in `web/` (ADR 0030). It has every screen from the prototype and reads the HTTP API through server functions when `LEADHUNTER_API_URL` is set on the web server, or sample data without it (ADR 0031, 0035, 0037). See [web/README.md](web/README.md) to run it, and [docs/screenshots/web](docs/screenshots/web) for what each page looks like.

```bash
cd web && pnpm install && pnpm dev   # http://localhost:3000, on sample data
```

### API and web with one command

Both read `.env` and use the same database as the CLI. See [ADR 0039](docs/adr/0039-full-stack-in-docker-compose-and-a-dev-script.md).

```bash
docker compose up --build   # everything in Docker: Postgres, the API on :8080, the web on :3000
./dev                       # Postgres in Docker, the API and `pnpm dev` (hot reload) on your machine
```

`docker compose up` needs only Docker; after changing code, run it again with `--build`. `./dev` needs JDK 21, Node 22+ and pnpm, rebuilds the jar each time it starts, and Ctrl+C stops the API and the web server. Both use ports 5432, 8080 and 3000, so run one at a time.

## Configuration

Copy [.env.example](.env.example) to `.env` in the project root and replace the values. The app reads `.env` on startup from the directory you run it in, so there's nothing to export. Real environment variables override `.env`, which is how Railway's settings take over in production. `.env` is in `.gitignore`. New knobs are added to `.env.example` over time, so after pulling, re-copy or merge from `.env.example` into your `.env`.

| Variable | Default | Notes |
|---|---|---|
| `APIFY_TOKEN` | none | Required for `campaign run`. From console.apify.com, Settings, API & Integrations |
| `AI_GATEWAY_API_KEY` | none | Vercel AI Gateway key. Check it with `llm test` |
| `LEADHUNTER_LLM_MODEL` | `google/gemma-4-26b-a4b-it` | Any gateway model id. Gemma is for testing, see [ADR 0017](docs/adr/0017-vercel-ai-gateway.md) |
| `LEADHUNTER_LLM_BASE_URL` | `https://ai-gateway.vercel.sh/v1` | Any OpenAI-compatible endpoint |
| `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` | docker-compose values | Railway sets these on its Postgres |
| `LEADHUNTER_APIFY_MAX_PLACES_PER_RUN` | 600 | Budget guard. Larger runs need `--allow-over-limit` |
| `LEADHUNTER_APIFY_ESTIMATED_USD_PER_PLACE` | 0.004 | Only for `--dry-run`. Set it from the actor's pricing page |
| `LEADHUNTER_USAGE_MONTHLY_BUDGET_USD` | 10 | The budget the bar in `usage` measures against |
| `LEADHUNTER_STAGE2_BATCH` | 25 | Qualified leads enriched per `campaign enrich` run |
| `LEADHUNTER_STAGE2_MAX_REVIEWS` | 10 | Recent reviews fetched per place for the complaint classification |

## Costs

The budget is $10 a month for scraping and LLM calls, see [ADR 0006](docs/adr/0006-two-stage-pipeline.md). Every Apify run stores what Apify reported it cost in `campaign_run.cost_usd`, failed and aborted runs included, and every LLM call stores the cost the gateway reported in `llm_call.cost_usd`. `usage` adds it up, see [ADR 0021](docs/adr/0021-track-usage-and-costs.md). LLM history starts from the day `usage` shipped. Always `--dry-run` a new campaign first.

## Railway

Create a Railway project with a Postgres database and a service built from the `Dockerfile`. The image's default command prints help and exits, so either:

- run the CLI from your laptop against Railway's database: `railway run java -jar backend/target/lead-hunter.jar leads list clinicas-luanda`, or
- set the service's start command to a campaign run and give it a cron schedule, with restart policy "Never".

## Tests

```bash
(cd backend && ./mvnw test)
```

Integration tests need Postgres. They use Testcontainers when Docker is running. Without Docker, point them at an empty database:

```bash
(cd backend && LEADHUNTER_TEST_JDBC_URL=jdbc:postgresql://localhost:5432/leadhunter_test ./mvnw test)
```

See [ADR 0016](docs/adr/0016-testing-strategy.md).
