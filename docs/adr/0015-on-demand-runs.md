# 0015. Run campaigns on demand

- Date: 2026-09-28
- Status: Accepted

## Context

Scheduled runs would spend the budget whether or not the owner has capacity to call anyone.

## Decision

Campaigns run when the owner starts them with `campaign run`. Places are deduplicated by Google place ID across runs, so re-running a campaign doesn't create duplicate leads. Scheduled runs can come later through Railway cron.

## Consequences

Spend follows actual need. The owner has to remember to refill the queue when it runs low.
