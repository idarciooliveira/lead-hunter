# 0021. Store what Apify and the LLM cost, and report it with `usage`

- Date: 2026-09-29
- Status: Accepted

## Context

ADR 0006 sets a $10 monthly budget for scraping and LLM calls. Apify spend was already stored in `campaign_run.cost_usd`, but only for successful runs, and LLM spend was not stored at all: `OpenAiCompatibleLlmClient` read the token counts and dropped them. There was no way to see what all runs together had cost.

What the providers tell us:

- Apify reports `usageTotalUsd` on the run, including failed and aborted runs, and bills those too. The first answer after a run ends can hold a preliminary figure. Apify's docs advise waiting about 10 seconds and reading the run again.
- The Vercel AI Gateway sends the cost of each call in `usage.cost` (a number, in USD) and in `choices[0].message.provider_metadata.gateway.cost` (a string). We checked this against a live response for `google/gemma-4-26b-a4b-it` on 2026-09-29.

## Decision

- Costs come from the provider's response. We do not keep a price table and do not compute cost from tokens.
- `llm_call` stores one row per completed LLM call: campaign (optional), purpose, model, tokens, `cost_usd`, the provider's generation id, and the raw `usage` JSON. `cost_usd` is null when the response has no cost. The raw JSON stays so the parsing can be fixed later without losing data.
- `RecordingLlmClient` wraps the real `LlmClient` and writes the row. Code that needs an LLM still depends on `LlmClient`. Callers say what a call is for with `LlmRequest.forCampaign(campaignId, purpose)`. A call that fails stores nothing. A row that fails to store is logged, and the answer is still returned.
- After a run ends, the Apify scraper waits `leadhunter.apify.cost-settle-delay` (10 seconds) and reads the run again before it takes the cost.
- A failed run stores its cost. `ScrapeException` carries it and `RunRepository.fail` writes it. When a run passes `max-run-time`, we abort it, because Apify keeps billing a run we stop watching, and record its cost.
- `usage` prints Apify and LLM spend, a budget bar for one month, and spend per campaign. `--month YYYY-MM`, `--campaign <slug>` and `--runs` filter and list. The budget is `leadhunter.usage.monthly-budget-usd`, default 10.
- A run or call with no cost is counted and shown as missing. It is never turned into zero.

## Consequences

LLM history starts on the day this ships. Earlier calls were not stored. Spend stays exact as long as the provider reports it, and follows model changes without config. A provider that sends no cost, such as a custom `LEADHUNTER_LLM_BASE_URL`, leaves gaps that `usage` reports. A BYOK call may report a gateway cost of zero, so the real spend would sit at the provider.

Each Apify run takes 10 seconds longer. Deleting a campaign deletes its Apify runs and their costs with it, while its LLM rows survive with no campaign. Month boundaries follow the machine's time zone.
