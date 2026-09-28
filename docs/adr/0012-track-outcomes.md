# 0012. Track contact outcomes per lead to calibrate scoring

- Date: 2026-09-28
- Status: Accepted

## Context

Without feedback the scoring rules from ADR 0007 stay guesses forever.

## Decision

The owner marks each lead after contact. Statuses: `NEW`, `CONTACTED`, `NO_ANSWER`, `INTERESTED`, `MEETING`, `PROPOSAL_SENT`, `WON`, `LOST`. A lost lead needs a reason: no budget, wrong person, already has a supplier, or not interested. Every status change is stored as an event with an optional note, so the history stays intact.

After 50 to 100 marked leads we compare which rules fired on leads that reached `MEETING` or better, and adjust weights in a new ADR.

## Consequences

The tool improves only if the owner marks outcomes. The CLI makes marking a single command to keep that friction low.
