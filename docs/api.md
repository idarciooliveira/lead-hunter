# HTTP API

JSON API for the web client (ADR 0031). Same domain code as the CLI; controllers hold no business rules. Errors mirror the CLI: `{ "message": "<text>" }` with 404 when the message starts with `no ` (not found) and 400 otherwise.

Run it: `SPRING_PROFILES_ACTIVE=web java -jar backend/target/lead-hunter.jar`. The CLI stays the default mode. `GET /api/health` is open; auth follows ADR 0032 (not yet enforced).

## Endpoints (implemented)

| Method | Path | Query | Notes |
|---|---|---|---|
| `GET` | `/api/health` | | `{"status":"ok"}` |
| `GET` | `/api/campaigns` | | Oldest first, with `totalCostUsd` per campaign |
| `GET` | `/api/campaigns/{slug}` | | 404 `no campaign '<slug>'. Run: campaign list` |
| `GET` | `/api/campaigns/{slug}/leads` | `stage=QUALIFIED\|BELOW_CUT\|EXCLUDED\|ALL` (default `QUALIFIED`), `limit` (default 20, max 200) | Same order as `leads list`: stage, score, reviews, id. Bad stage → 400 |
| `GET` | `/api/leads/{id}` | | Full lead card: score, `breakdown[{code,points,reason}]`, `whatsappLink`. 404 `no lead with id <id>` |
| `GET` | `/api/company` | | The company profile. 404 `no company profile yet. Run: company setup` |
| `GET` | `/api/usage` | `month=YYYY-MM`, `campaign=<slug>` | Totals (`apify`, `llm`, `byCampaign`, `totalUsd`) plus `budgetUsd`. Bad month → 400, unknown campaign → 404 |
| `GET` | `/api/usage/entries` | same, plus `limit` (default 30, max 200) | Newest runs and calls first, like `usage --runs` |
| `PATCH` | `/api/leads/{id}` | `{status, lostReason?, note?}` | Marks a contact outcome, like `leads mark` (ADR 0012, 0020). `LOST` needs one of `NO_BUDGET`, `WRONG_PERSON`, `HAS_SUPPLIER`, `NOT_INTERESTED`, `NOT_NOW`; a lost reason needs `LOST`; a worked lead can never go back to `NEW`. 404 `no lead with id <id>`, 400 otherwise |
| `GET` | `/api/campaigns/{slug}/runs` | | Job history, newest first: one row per job, from the UI or the CLI, plus the standalone rows older CLI versions wrote (ADR 0033, 0036) |
| `GET` | `/api/runs/{id}` | | One job as the client polls it: `kind` (`SCRAPE`, `ENRICH`, `DRY_RUN`), `status` (`RUNNING`, `DONE`, `FAILED`), `startedAt`, `done` (places found or leads enriched so far, then the verdict), `total` (known total for enrichment, null for scrapes), `costUsd` (Apify plus, for enrichment, the LLM calls it made; null when a finished part has no known cost), `error`. 404 `no run with id <id>` |
| `POST` | `/api/campaigns/{slug}/runs` | `dryRun` (default `false`), `allowOverLimit` (default `false`) | Starts a scrape job: 202 with the `RUNNING` job, like `campaign run`. With `dryRun` it returns the free estimate instead (`requests[{location, terms, maxPlaces}]`, `maxPlaces`, `estimatedMaxUsd`, `overLimit`, `dryRunId`), like `campaign run --dry-run`. `allowOverLimit` is the explicit opt-in past the limit. Second start while one runs, from the UI or the CLI → 409; over the limit without the flag → 400 with the budget message; too many jobs waiting → 503; 404 `no campaign '<slug>'. Run: campaign list` |
| `POST` | `/api/campaigns/{slug}/enrichment` | `batchSize?`, `maxReviews?`, `dryRun` (default `false`) | Starts an enrichment job, same shape as a scrape, like `campaign enrich`. Batch and review counts default like the CLI. With `dryRun` it returns the free estimate (`pending`, `batch`, `maxReviews`, `dryRunId`). Nothing waiting → 400 `nothing to enrich: ...` |

## Writes (planned)

`POST/PUT /api/campaigns[/{slug}]` and `PUT /api/company` with the CLI validation (ADR 0029).

## Web client

Each feature's `api.ts` calls these paths through `lib/http.ts` when `VITE_LEADHUNTER_API_URL` is set, and reads the fixtures otherwise. The Playwright `integration` project serves a mock of this contract and proves the pages render from real HTTP responses.
