# 0040. Write one pitch per lead during enrichment, and drop it when it invents a number

- Date: 2026-10-06
- Status: Accepted

## Context

ADR 0007 says the LLM reads reviews and writes pitches, and ADR 0019 says a pitch may only claim what the company profile can prove. Review analysis exists (ADR 0027). Pitch writing does not, and the web client shows `lead.pitch` in the lead card, the today queue and the WhatsApp link, today from sample data only.

A message that quotes a price, a delivery time or a result we never promised is worse than no message, and the person who sends it will not recheck every figure.

## Decision

- `campaign enrich` writes one pitch for each lead it enriches, after the stage 2 score is saved. Leads that already have a pitch are not touched by a re-run, because enrichment skips enriched leads.
- `PitchWriter` depends on `LlmClient` only. It sends the company profile, the campaign answers (service, price, hook, tone), the lead facts (name, category, neighbourhood, rating, reviews, the reasons in the score breakdown, the complaint kinds) and the case studies. The calls are recorded with purpose `pitch`, so `usage` counts them.
- The prompt asks for a short WhatsApp message in Portuguese from Angola, with one hook taken from the lead's own evidence, at most one case study from the list, no price or delivery time that is not in the data, and a closing question.
- A guard compares the digits in the answer with the digits in the prompt. If the pitch contains a number the prompt did not, it is dropped and the lead keeps no pitch. A failed LLM call or an empty answer also leaves no pitch. Neither fails enrichment.
- The pitch never changes the score (ADR 0007).
- One pitch per lead, no variants. `leads pitch <id>` and `POST /api/leads/{id}/pitch` write a new one for a lead that has none or whose pitch the user dislikes.
- Migration V7 adds `lead.pitch` and `lead.pitch_model`. The model is the one the gateway reports.

## Consequences

Easier: every number in a pitch traces to the profile, the campaign or the lead. The web client reads one field. Spend stays inside the usage report.

Harder: the guard rejects honest pitches that round a number or spell it out ("dois anos"), and a lead can end up with no pitch. We accept that and regenerate by hand. The guard checks digits, not meaning, so a wrong claim without a number still gets through. Leads enriched before this change have no pitch until `leads pitch` runs for them.
