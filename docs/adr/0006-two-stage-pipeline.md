# 0006. Run a two-stage pipeline to stay within a $10 monthly data budget

- Date: 2026-09-28
- Status: Accepted

## Context

The budget for scraping and LLM calls is $10 a month, with Railway hosting on top. At 30 to 40 contacts a week the owner needs roughly 300 to 400 qualified leads a month. Reviews, website crawling, and LLM calls cost money or time per lead, and most places found in a broad search are poor fits.

## Decision

- Stage 1, discovery. Scrape basic place data with no reviews and no images. Apply exclusion rules and a preliminary score using only what the scrape returns. Keep the top share of candidates as `QUALIFIED` and mark the rest `DISCARDED` with a reason.
- Stage 2, enrichment. Only qualified leads get reviews, a website crawl, a final score, and a generated pitch.

Every Apify run records its cost in `campaign_run.cost_usd`, so spend is visible per campaign. `campaign run --dry-run` prints the planned searches and maximum place count without calling Apify.

## Consequences

Most of the budget goes to leads we might actually contact. A good lead that looks bad in stage 1 can be discarded too early. Discard reasons are stored, so we can audit that.
