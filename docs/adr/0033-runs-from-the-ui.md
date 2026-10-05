# 0033. Start runs from the UI as background jobs that the client polls

- Date: 2026-10-04
- Status: Proposed
- Amends: [0010](0010-postgres-flyway-jdbcclient.md), [0015](0015-on-demand-runs.md)

## Context

ADR 0015 says campaigns run on demand, and ADR 0010 says there is no job queue in v1. A campaign run or an enrichment can take minutes, longer than a sensible HTTP request. Starting one spends real money against the $10 monthly budget (ADR 0006), and `BudgetExceededException` already guards the pipeline.

## Decision

- `POST /api/campaigns/{slug}/runs` returns 202 with a run id. The work runs on a bounded executor inside the JVM. Enrichment starts the same way.
- `GET /api/runs/{id}` returns status, counts and cost so far from `campaign_run`. The UI polls it. Server-sent events and WebSockets wait until polling proves insufficient.
- At most one run per campaign at a time. A second start returns 409.
- The UI shows an estimate and asks for confirmation before any start. A dry run mirrors the CLI's `--dry-run` and costs nothing.
- Budget errors reach the UI as a clear message, not a generic failure.
- A run interrupted by a restart is marked failed on startup. There is no resume and no external queue.

## Consequences

Runs survive closing the browser but not a restart of the application. We accept that for one owner and on-demand use. Each run row needs a status that the UI can read, which may need a Flyway migration. Spend stays on demand, now behind a button, so the confirmation step matters.
