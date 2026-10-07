# 0047. Serve the stage 2 evidence on the lead card

- Date: 2026-10-07
- Status: Accepted

## Context

Enrichment (ADR 0027, 0028) stores a website crawl per place, the complaint kinds the model found, and a combined score breakdown. The API returned only the combined breakdown and the complaint kinds, so the web hardcoded `stage2: null`, an empty website audit and null complaints, and only the sample data showed them (issue #21).

The model returns kinds (`contact`, `booking`, `waiting`), not counts or quotes. The reviews are stored, but nothing says which review raised which kind.

## Decision

- `breakdown` on a lead holds the stage 1 items. A new `stage2Breakdown` holds the stage 2 items, and is null until enrichment ran. The split uses `Stage2Scorer.isStage2Code`, so the scoring code stays the only place that knows its codes. The total `score` is unchanged.
- `GET /api/leads/{id}`, `PATCH /api/leads/{id}` and `POST /api/leads/{id}/pitch` also return `websiteCrawl`: the newest `website_crawl` row (`url`, `reachable`, `https`, `mobileFriendly`, `stale`, `httpStatus`, `error`, `crawledAt`), or null. Lists return it null, to avoid one query per row.
- The API sends crawl facts. The web words them as audit rows and names the complaint kinds. It computes no score.
- Complaints show the kinds only. The web has no mention counts or quotes to show, so those fields are empty until the classifier records them per review. That needs a new prompt and a migration, and gets its own ADR.
- No migration.

## Consequences

Easier: the lead page shows the stage 2 breakdown, the website audit and the complaint themes with a real API.

Harder: clients that summed `breakdown` to reach `score` now also need `stage2Breakdown`. The complaints card has no counts or quotes against a real API, unlike the sample data.
