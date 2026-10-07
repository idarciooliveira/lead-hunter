# 0049. Rank all leads on the server

- Date: 2026-10-07
- Status: Accepted

## Context

The leads page and the command palette need every lead across campaigns. With the API, the web called `GET /api/campaigns` and then `GET /api/campaigns/{slug}/leads?stage=ALL&limit=200` once per campaign, merged the answers and ranked them in the browser. A campaign with more than 200 leads lost the rest without any error, the number of calls grew with the campaigns, and the rank was a number the UI computed (ADR 0007 keeps scores and their derivations in the API). A single lead fetch has no rank, so the card and the list could disagree.

## Decision

- `GET /api/leads` returns the leads of the caller's organization across campaigns. The order is non-excluded first by score, then reviews, then id, with the excluded last. This is the order the web used.
- Each row is the lead shape plus `rank`: the position in that order among the non-excluded leads, and null for the excluded. The database computes it with `row_number()`.
- `limit` defaults to 500 and stops at 2000. The web asks for 2000. Past that, a page needs paging, which a PME's lead list does not need yet.
- The today queue keeps its own order. Its rank in the web is the position in the queue the API returned.
- `GET /api/leads/{id}` still has no rank. It would need a count over the organization for each fetch.
- No migration.

## Consequences

Easier: one call and one query per page, no silent cut at 200 per campaign, and no ranking code in the web.

Harder: more than 2000 leads in an organization are cut at the end of the ranking, the lowest scores. The rank of an enriched lead moves when its score changes, so a lead's rank on the list and on its card can differ until the card gets one.
