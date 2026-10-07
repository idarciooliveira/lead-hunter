# 0048. Serve the campaign funnel from the stored leads and jobs

- Date: 2026-10-07
- Status: Accepted

## Context

The campaign page draws the pipeline of ADR 0006 as five cards: scrape, filter, stage 1 score, cut and enrich. The API sent no funnel, so the web hardcoded `funnel: null` and only the sample data showed the cards (issue #21).

Every number already sits in the database. Each place a campaign finds is a lead, stage 1 marks the dropped ones `EXCLUDED` with a reason, the cut marks `QUALIFIED`, enrichment sets `enriched_at`, and the jobs hold their costs.

## Decision

- `GET /api/campaigns/{slug}` returns `funnel`, built by `CampaignFunnel` from the leads and the job history. The list and the other campaign responses leave it null, which avoids more queries per row.
- `scraped` is the number of leads, so a place two searches return counts once. `kept` is `scraped - excluded`. `qualified` is a lead count. `enriched` counts the qualified leads that are enriched, so a lead a rerun demoted drops out of it. `cutShare` is the campaign's `qualifyShare`.
- `excludedBy` holds the three most common exclusion reasons with their counts. The web words the filter note from them and computes no count.
- `scrapeCostUsd` and `enrichCostUsd` add the known cost of the scrape and enrich jobs, part by part. A part whose price is not known yet adds nothing, so a running job's cost grows as its parts finish, and a failed part without a price does not hide the priced ones. Dry runs are ignored.
- `enriching` is true while an enrich job is running.
- The funnel is null until the campaign has a real (non dry-run) job.
- No migration and no new storage.

## Consequences

Easier: the campaign page shows its funnel with a real API, and the CLI and any script can read the same counts.

Harder: `scraped` is the distinct places kept, not the raw rows the scraper returned, so it can be lower than the sum of the runs' `places_found`. The filter note shows the backend's exclusion reasons as written, in English.
