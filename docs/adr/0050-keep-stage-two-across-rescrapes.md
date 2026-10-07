# 0050. Keep stage 2 across rescrapes, and audit only the current website

- Date: 2026-10-07
- Status: Accepted

## Context

Each campaign scrape upserts every place and saves a stage 1 result per lead (ADR 0006). Enrichment (ADR 0027) appends the stage 2 items to the lead's breakdown and sets `enriched_at`. Before this change, a rescrape replaced the breakdown with the stage 1 items and left `enriched_at` set. The enriched lead then lost its stage 2 items, the card said stage 2 had not run, and the enrichment queue skipped it.

A rescrape can also change a place's website. The crawls of the old site stay in `website_crawl`, so the lead card could show the old site's audit as the current one. ADR 0047 showed the newest crawl, which is the old site's crawl after such a change.

## Decision

- A rescrape keeps the stage 2 items and `enriched_at` of an enriched lead. The new stage 1 items go beside them, and the score is their sum. Enrichment is not run again, because it costs a review fetch and LLM calls per lead.
- A rescrape that excludes the lead, or that gives its place another website, clears `enriched_at` and `complaint_kinds`. The breakdown holds only stage 1, so the lead returns to the enrichment queue when it qualifies. A website counts as changed when the newest crawl read another site, or when the place has an own website that was never crawled. A place with no own website and no crawl is not changed.
- The lead card's website audit reads the newest crawl of the place's current website. Crawls of an earlier website are never shown.
- The API shape of ADR 0047 stays. Only the crawl the card reads changes.

## Consequences

Easier: a rescrape no longer hides an enriched lead's stage 2, and the card and the queue agree with the stored data. A lead whose website changed is audited on the new site once it is enriched again.

Harder: stage 2 can be stale between enrichment runs. The reviews and the site are read again only when the lead is enriched again, which is a manual run, and a website change is seen only at the next rescrape.
