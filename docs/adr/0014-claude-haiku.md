# 0014. Use Claude Haiku 4.5 for review analysis and pitches

- Date: 2026-09-28
- Status: Accepted

## Context

Stage 2 needs to read reviews in Portuguese, find complaints a software product would fix, and write a short WhatsApp message and call opener. It runs on a few hundred leads a month within the budget in ADR 0006.

## Decision

We use Claude Haiku 4.5, model ID `claude-haiku-4-5-20251001`, through the Anthropic API. At about $1 per million input tokens and $5 per million output tokens, one lead costs under half a cent. The API key lives in `ANTHROPIC_API_KEY`.

## Consequences

LLM cost stays at a couple of dollars a month. If pitch quality is poor we can move to a larger model for pitches only, with a new ADR.
