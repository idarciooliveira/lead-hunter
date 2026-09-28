# 0020. Add "not now" as a reason for a lost lead

- Date: 2026-09-28
- Status: Accepted
- Amends: [0012](0012-track-outcomes.md)

## Context

ADR 0012 fixes the reasons for a `LOST` lead: no budget, wrong person, already has a supplier, not interested. A lead who says "fale comigo em Março" fits none of them and would be recorded as not interested, which hides a future deal. ADR 0019 also adds a "why now" question to campaigns, and without a timing reason we can't tell whether that bet held.

## Decision

`LOST` gets a fifth reason, `NOT_NOW`, with an optional date to come back. `lead mark` isn't built yet, so its first version and its migration include this reason from the start.

## Consequences

Leads lost on timing can come back into the daily queue later. The recalibration after 50 to 100 marked leads can separate "wrong lead" from "right lead, wrong moment".
