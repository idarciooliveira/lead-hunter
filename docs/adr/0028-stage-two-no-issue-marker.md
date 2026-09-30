# 0028. Mark a clean stage-2 run with a zero-point STAGE2_NO_ISSUES code

- Date: 2026-09-30
- Status: Accepted

## Context

ADR 0027 stores the stage 2 result by appending its items to the stage 1 breakdown, and ADR 0007 only writes a breakdown line when a rule fires and earns points. A healthy website and quiet reviews earn nothing, so an enriched lead whose stage 2 found no issues keeps a breakdown identical to a lead stage 2 never touched. Only `enriched_at` in SQL told the two apart; `leads show` could not answer "did stage 2 run?".

## Decision

When stage 2 executes and fires no point-earning rule, `Stage2Scorer` appends one explicit `STAGE2_NO_ISSUES` item worth 0 points to the breakdown, with a reason saying why nothing was added: "Website reachable, HTTPS, mobile-friendly; no review complaints" for a clean crawl, "No website to crawl; no review complaints" when there was nothing to crawl. A lead stage 2 never ran keeps no marker. No ADR 0007 weight changes and the score total is unaffected.

## Consequences

Easier: `leads show` (and anything reading `score_breakdown`) distinguishes "stage 2 checked, no issues" from "never enriched" without SQL, and the enrich progress line names the marker instead of "(no change)".

Harder: the breakdown now carries one non-scoring code, so any consumer that sums or counts breakdown lines must ignore 0-point items.
