# 0001. Record architecture decisions

- Date: 2026-09-28
- Status: Accepted

## Context

Lead Hunter starts as a solo project. Decisions made in chat get lost, and six months from now nobody will remember why the scraper is Apify or why scoring is plain Java. We want each decision and its reasons on record, including the ones we later reverse.

## Decision

We keep Architecture Decision Records in `docs/adr/`, one Markdown file per decision, numbered in order. Each record follows `template.md`: context, decision, consequences.

Records are never edited to change a decision. A new record replaces an old one, and the old one gets the status `Superseded by NNNN`. Typos and clarifications are fine to fix in place.

Every change that picks a library, a data source, a storage model, a scoring approach, or a product scope gets a record in the same pull request.

## Consequences

Anyone reading the repo, including an AI assistant, can see why things are the way they are. Writing a record takes ten minutes per decision, which is a cost we accept.
