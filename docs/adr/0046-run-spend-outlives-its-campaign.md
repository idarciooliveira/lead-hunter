# 0046. Keep run spend after its campaign is deleted, and admit jobs one at a time

- Date: 2026-10-07
- Status: Accepted

## Context

ADR 0044 says deleting a campaign must not remove spend from the month, and leaves the mechanism to the migration that adds `org_id`. Until now `campaign_run` rows were deleted with their campaign, and the usage breakdown only listed campaigns that still exist.

The budget check reads the month's spend and then inserts the job row. Two jobs of different campaigns that start at the same moment could both read the same spend and both pass, together going over the budget or the install cap. The unique index on running jobs only covers one campaign.

A job reserves its estimate until it ends. If the check only counted jobs that started this month, a job started on the last evening of a month would stop reserving anything at midnight UTC while it still had runs to start.

## Decision

- `campaign_run` carries `org_id`, and its `campaign_id` is nullable with `on delete set null`. Deleting a campaign keeps its runs and their cost in the organization's month. `llm_call` already worked this way (ADR 0021).
- The usage breakdown groups runs of deleted campaigns and LLM calls made outside a campaign into one row with no slug, so the rows add up to the total. The CLI prints it as `(deleted or no campaign)` and the web as "Sem campanha ou apagada".
- A job is admitted under one Postgres transaction-level advisory lock shared by every organization. The budget check and the insert of the job row run inside that transaction, so starts are checked one after the other. One key covers the install cap too.
- A running job reserves the part of its estimate its runs do not cover yet, whatever month it started in.
- The usage API returns `committedUsd`, this UTC month's spend plus reservations, which is what the check counts. The CLI and the web show it next to the reported spend when it is higher.
- `usage --month` reads calendar months in UTC, like the budget check and the API, so the CLI bar and a refusal count the same window.
- The per-review and per-lead estimates must be more than zero, and the app refuses to start otherwise.

## Consequences

Spend can no longer go down by deleting a campaign, and a deleted campaign's history shows only as part of the shared row. Job starts across all organizations wait for each other for the length of one budget query, which is fine at the rate jobs start. A job that spans two months counts its remaining estimate in the second month even though its first runs count in the first.
