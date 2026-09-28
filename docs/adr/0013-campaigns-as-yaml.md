# 0013. Define campaigns as YAML files built on the 10 onboarding questions

- Date: 2026-09-28
- Status: Accepted

## Context

Each search starts from 10 questions about the business: what it sells, who buys, where, reference clients, size, visible pain, exclusions, decision maker and channel, proof, and weekly capacity. The CLI is non-interactive, per ADR 0009.

## Decision

A campaign is a YAML file with two parts:

- `answers`: the 10 questions, in plain language. Stored as `jsonb` and used later by the LLM for pitches.
- `search`: the concrete search terms, locations, places per search, target sector keywords, and exclusions.

`campaign template` prints an example file. `campaign create --file` validates it and saves it. Search terms are written by hand for now. Generating them from the answers with the LLM is a later step.

## Consequences

Campaign files can live in git next to the code, so changes to targeting have history too. Editing YAML is fine for one technical user. A product version would need a form.
