# 0036. Hold one job per campaign with a parent row and a Postgres advisory lock, for the CLI and the API alike

- Date: 2026-10-05
- Status: Accepted
- Amends: [0033](0033-runs-from-the-ui.md)

## Context

ADR 0033 allows one run per campaign at a time and marks runs a restart interrupted as failed on startup. The first version had three gaps. The CLI and the web server share one database, and the startup cleanup ran on every boot, so any CLI command, even `usage`, failed the live jobs of a running web server. `campaign run` only checked for a running job once, before it started, and `campaign enrich` never checked, so a UI start could slip in while the CLI worked and break it with a raw database error. And a job row only said RUNNING; nothing told a live job from one whose process was killed.

## Decision

- `campaign run` and `campaign enrich` run as jobs, like the UI starts: a parent row in `campaign_run`, children per search, from start to verdict. `RunJobService` runs them in the foreground for the CLI and on the executor for the API.
- A job holds a lease: a session-level Postgres advisory lock on its id, taken in the statement that inserts the parent row and held on its own connection until the verdict is written. If the process dies, Postgres ends the session and frees the lock.
- A RUNNING job whose lock can be taken has no live owner and is marked failed, with any RUNNING children. This runs before every start for that campaign, and for every campaign when the web server boots. Live jobs, in this or any other process, keep their lock and are left alone.
- A waiting background job already holds its lease and so a pooled connection. The executor queue is four jobs, under Hikari's ten connections. A start past that fails its job at once and returns 503.

## Consequences

A killed CLI run or web server no longer blocks its campaign, and booting one process never touches another's jobs. The CLI and the UI now write the same history, so the UI's run list shows CLI runs as jobs too. Each running job keeps one database connection for its whole run, which caps how many jobs can wait. Advisory locks tie the design to Postgres, which ADR 0010 already chose.
