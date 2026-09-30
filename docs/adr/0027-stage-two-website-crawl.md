# 0027. Enrich qualified leads with a website crawl and stage 2 scoring

- Date: 2026-09-30
- Status: Accepted

## Context

Stage 1 (ADR 0006) ranks places using only the Maps scrape. The rules that decide whether a business is worth a call — a real website that is broken, stale, or missing the thing we sell — need the site itself, and the first rules of ADR 0007 (broken website +25, reviews complain about contact, booking or waiting +20) cannot fire in stage 1. Only `QUALIFIED` leads get enrichment, which keeps the crawl and the LLM calls inside the $10 monthly budget.

The LLM plumbing (`LlmClient`, `RecordingLlmClient`, per-call cost tracking) and the usage report already exist, so stage 2 only needs to consume them.

## Decision

- Stage 2 runs on demand with `campaign enrich <slug>`, never automatically after `campaign run`. It processes `QUALIFIED` leads up to a configurable batch (`LEADHUNTER_STAGE2_BATCH`, default 25) so a run cannot blow the month's budget in one step. Already-enriched leads are skipped, so re-running is safe.
- The website crawl is plain Java 21 (`java.net.http.HttpClient`), no external crawling service. Per lead it fetches the homepage, follows one hop to a `contacto`/`contact`/`sobre` page, and caps any single body at 512 KB and total time per lead at 20 seconds. It records, per lead: reachable or not, HTTPS or plain HTTP, mobile-friendly or not (viewport + @media evidence), stale or not (copyright year older than two years, or no update hints on a cached copy), and the content hash so a re-crawl can tell "unchanged" from "refreshed".
- The results feed `Stage2Scorer`, a second explicit Java rule scorer: broken or unreachable +25, not mobile-friendly +25 once, HTTPS missing +25 once (a site scores at most one website rule), stale +25. No browser, no JavaScript rendering; if meta-JS-only detection is needed later, that is a new ADR.
- Reviews for qualified leads come from the existing Apify Google Maps actor, saved as review rows keyed by place. The LLM (through `LlmClient`, the configured gateway model) classifies each lead's recent reviews into complaint categories — contact, booking, waiting, none — and stores the classes plus the model used on the lead. Classification never changes the score by itself: it only supplies one ADR 0007 rule (+20 when complaints cover contact, booking or waiting), so the score stays auditable end to end.
- Stage 2 items append to the stage 1 breakdown; the final score is the sum, clamped 0 to 100, per the Stage1Scorer convention. Nothing in ADR 0007's stage 1 weights changes.
- Schema is additive in one new Flyway migration: `website_crawl` (one row per crawl attempt, with the booleans above and the error), and `place_review` for scraped reviews (place, star, text, published date). Never edit earlier migrations.

## Consequences

Easier: every stage 2 point has a stored piece of evidence; a crawl costs time, not an API fee, so the crawl only needs the LLM-count guard the usage command already reports; skipped leads on re-run make the batch loop natural.

Harder: some Luanda sites block plain HTTP clients or are JavaScript-only, and we accept that as "not reachable" with the error stored; viewport-keyword mobile detection is a heuristic and will misclassify a few sites, which we revisit with marked-lead data (ADR 0012) rather than more machinery.

## References

- [0006](0006-two-stage-pipeline.md) — two-stage pipeline
- [0007](0007-rule-based-scoring.md) — stage 2 rules and the LLM's role
- [0016](0016-testing-strategy.md) — crawl tested against a local fixture server, never real sites
