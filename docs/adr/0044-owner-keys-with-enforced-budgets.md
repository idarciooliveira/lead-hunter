# 0044. Run every organization on the owner's keys, with budgets that refuse runs

- Date: 2026-10-06
- Status: Accepted

## Context

ADR 0006 set a $10 monthly budget for one owner. ADR 0021 records what every Apify run and LLM call costs, but the budget only draws a bar in `usage`; nothing stops a run. With organizations (ADR 0043), every run would spend the owner's Apify and AI Gateway money. Issues #16 and #17 asked for keys entered in the UI, which would mean storing secrets in the database, against the rule that secrets come only from environment variables.

Deleting a campaign today deletes its `campaign_run` rows and the Apify cost on them (ADR 0021), so a month's spend can go down.

## Decision

- The Apify token and the AI Gateway key from the environment serve every organization. Organizations do not bring their own keys for now; that would store secrets in the database and needs its own ADR.
- Each organization has a monthly budget in USD. It defaults to `leadhunter.usage.monthly-budget-usd`. Only the operator sets it, from the CLI, because the operator pays; organization admins see it.
- `LEADHUNTER_TOTAL_MONTHLY_BUDGET_USD` caps the spend of all organizations together.
- Before a scrape or an enrichment starts, the pipeline adds the job's estimate to the organization's spend this month. If the total passes the organization's budget or the install cap, the run is refused with an error that names the limit and both amounts. The CLI and the API go through the same check.
- A run whose cost is missing counts at the estimate stored when it started, never as zero.
- Deleting a campaign no longer removes spend from the month. The migration that adds `org_id` picks the mechanism.
- The check runs when a job starts. A job that is already running is never stopped halfway.
- The model is one of a short list the owner has tested, chosen per organization. The default is `LEADHUNTER_LLM_MODEL`.

## Consequences

The owner carries all the cost, bounded by the install cap. A run can still overshoot by the gap between its estimate and its real cost. There is no billing; charging organizations needs a new ADR. `usage` and the web usage page report per organization, and the operator gets a view across all of them.
