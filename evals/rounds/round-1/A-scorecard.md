# Agent A — Scorecard, Round 1

- Harness: `bash evals/harness.sh round-1` — all four tests exited 0 (company-create, company-update, campaign-new, campaign-run-dry).
- Transcripts reviewed end to end: `evals/runs/round-1-*.txt`.
- Simulated user: Portuguese-speaking PME owner (TchiowaLabs), not a developer.

## Overall scores

| Dimension | Score (0-10) | One-line justification |
|---|---|---|
| Experience (flow, guidance, errors) | **6** | Well-sectioned wizard with sensible defaults and "Next:" pointers, but end-of-wizard guidance contradicts itself, an Enter-to-overwrite confirm shows a `[y/N]` default it does not honour, and a raw `null` leaks into the update summary. |
| Question quality (clarity, examples, defaults, jargon) | **6** | Every question has a concrete Portuguese example and a bracketed default, but the campaign wizard's 1/11..11/11 numbering does not match the actual prompt order, w/d signal codes are cryptic, and "search terms" vs "keywords that mark a target sector" overlap confusingly. |

No previous round to compare against (round 1 baseline).

## Question-by-question table — `company setup` (create)

| Id | What it asks | Verdict | Reason | Evidence (round-1-company-create.txt) |
|---|---|---|---|---|
| C1 | Company name | good | Plain, unambiguous, first question is the right one to ask first | `C1/10  Company name` |
| C2 | One-sentence phone intro | good | Concrete example in the customer's own language makes the format obvious | `e.g. Somos a X, fazemos sites e sistemas para PMEs em Luanda.` |
| C3 | Services list (name \| price \| delivery) | good | Format is spelled out per line and the optional field is marked | `One per line: name \| price range in Kz \| delivery time (optional). An empty line finishes.` |
| C4 | Entry offer | good | Numbered menu rebuilt from C3 with prices shown; default offered | `1. website pacote basic (165.000 kz) ... Number [1] >` |
| C5 | Where you serve in person | good | Sensible `[Luanda]` default and explains the consequence | `municipalities or provinces; campaigns search here by default [Luanda]` |
| C6 | Current clients | good | Explains *why* it matters (exclusion from leads) — rare and excellent | `Current clients. They never show up as leads, in any campaign.` |
| C7 | Provable results (cases) | confusing | Caps at 2 before explaining what a case even is; no example of a case | `Up to 2 now. Add more later in company.yml. Add a case? [y/N]` |
| C8 | Objections + answers | good | Canned objections answered one at a time is the right pattern for this user | `"É caro" > "Já tenho Instagram ou Facebook" > ...` |
| C9 | Weekly contact capacity | good | Clear scope ("across all campaigns"); default given | `How many leads can you contact per week, across all campaigns? [35]` |
| C10 | Quarterly target | good | Optional status is explicit, Enter skips | `C10/10 Optional. Target for this quarter. Enter skips.` |

## Question-by-question table — `company setup` (update)

| Id | What it asks | Verdict | Reason | Evidence (round-1-company-update.txt) |
|---|---|---|---|---|
| C1-C9 defaults | Keep-in-brackets re-ask | good | Every previous value shown in brackets; "Press Enter to keep" is stated up front | `Update the company profile. Press Enter to keep the value in [brackets].` |
| C10 | Target summary | bad | Raw `null` shown to a non-technical user | `[null new clients, 500000 Kz]` |
| overwrite confirm | File overwrite | bad | Display says `[y/N]` but a bare Enter saved anyway — the default shown is not the default honoured | `campaigns/company.yml already exists. Overwrite it? [y/N] > Saved the company profile for TchiowaLabs.` |
| C8 (update) | Objection answers | confusing | Scripted answers misalign with the canned objections: the script's sobrinho-related lines were consumed as answers to "Já tenho Instagram ou Facebook" and "Agora não tenho tempo", storing objection→answer pairs the user never intended. Script/user misalignment, not a CLI crash — harness blank-line buffer absorbed it. | script line 16 `Já tenho um sobrinho que faz sites` consumed at prompt `"Já tenho Instagram ou Facebook" >` |

## Question-by-question table — `campaign new` + dry-run

| Id | What it asks | Verdict | Reason | Evidence (round-1-campaign-new.txt) |
|---|---|---|---|---|
| name/slug | Campaign name + short id | good | Slug auto-derived from the name; constraint stated | `Short id for commands — lowercase and dashes [clinicas-dentarias-em-luanda]` |
| 1/11 | Sector | good | Two-word example matches the persona's vocabulary | `Sector, in a word or two — e.g. clínicas dentárias` |
| 2/11 | Sector problem bet | good | "In the customer's words" plus a full-sentence example | `e.g. Perdemos marcações porque os pacientes só conseguem ligar` |
| 3/11 | Service pitched | good | One-service-per-campaign rationale given; menu from company profile | `One per campaign, so you can tell which one sells.` |
| 4/11 | Free hook for replying | good | Concrete examples; default carried from company profile | `[Uma análise gratuita do vosso perfil no Google Maps]` |
| 5/11 | Why buy now | good | Marked optional with seasonal examples | `Optional. Why would they buy now rather than next year? e.g. recently opened` |
| 6/11 | Maps signals w/d/Enter | confusing | Letter codes are dense; the parenthetical explanation helps but six prompts in a row with no per-signal example | `Google Maps signals. w = wanted, d = disqualifying, Enter = doesn't matter.` |
| 7/11 | Minimum reviews | good | Consequence stated; 0 default excludes nothing, which is the safe default | `Minimum Google reviews for a business that can pay — places below it are excluded [0]` |
| 8/11 | Phone routine | good | Default in Portuguese, ready to use as-is | `[A recepção atende; pedir o dono ou gerente.]` |
| 9/11 | Sector objections | good | Clarifies company-level objections are already included (no duplicate work) | `The company objections are already included.` |
| 10/11 | Proving case | good | Honest empty state with recovery path | `The company profile has no cases. Add them with company update.` |
| goal block (unnumbered) | Meetings / won / end date / leads-week / N-M stop rule | confusing | Six prompts with no question ids sitting between 10/11 and 11/11; the N/M compound rule takes two reads | `Stop or rethink when fewer than N leads are interested after M contacts — N, interested [2] M, contacted [20]` |
| 11/11 | Tone | good | Strong Portuguese default | `[Formal, em português (o senhor / a senhora)]` |
| search: terms | Google Maps search terms | confusing | Later re-appears as "Keywords that mark a target sector" defaulted to the same words (`[clínica; clínica dentária]`); the difference between the two fields is never explained | `Google Maps search terms — e.g. clínica, clínica dentária` vs `Keywords that mark a target sector — matched as whole words in the name or category [clínica; clínica dentária]` |
| search: locations | Locations, one scraper run each | good | Ties the input to a cost consequence | `Locations, one scraper run each — e.g. Talatona, Luanda, Angola [Talatona, Angola]` |
| search: max places | Max places per term per location | good | Bounded numeric with default | `Max places per term per location [40]` |
| search: excludes | Sector keywords / exclude words / extra names | good | Reassurance that current clients are always excluded | `current clients from the company profile are always excluded` |
| dry-run | `campaign run --dry-run` output | good | Shows plan + real cost estimate before spending; concise | `Up to 80 places, about $0.32 at the configured price per place.` |

## Flow-level UX findings

1. **Contradictory end-of-wizard guidance.** `campaign-new` says `Saved campaign 'clinicas-dentarias-em-luanda'.` and then `Edit it for finer settings, then: campaign create -f campaigns/...yml`. The dry-run works without any `campaign create`, so the second instruction reads as a required step that is not. Same pattern in `company-create` (`Edit it, then: company update -f campaigns/company.yml`).
2. **Numbering does not match flow.** Campaign wizard promises `1/11..11/11`, but at least six goal prompts and seven search prompts are unnumbered, and 11/11 appears after the goal block. A user counting progress loses track.
3. **Prompt spacing.** Prompts run on immediately after the previous `> ` input marker (e.g. `> C2/10 How do you introduce...`), making the transcript — and presumably the live terminal — hard to scan.
4. **Language mix.** Instructions are English; examples and defaults are Portuguese. For the persona (reads Portuguese, not a developer) the load-bearing prompts are in their weaker language.
5. **Good:** capacity accounting (`10 of 10 free`), client-exclusion reassurance, cost estimate in the dry-run, and every wizard ending with a `Next: ...` line.

## Script/wizard alignment notes (report only, nothing edited)

- `evals/scripts/company-update.txt` lines 16-18 answer the canned objections of C8 with mismatched texts (sobrinho and tempo answers land on Instagram and tempo prompts). The harness blank-line buffer turned this into a silent pass; the stored profile now contains objection→answer pairs the simulated user did not intend.
- `evals/scripts/campaign-new.txt` line 18 (`1`) re-answers "Won clients" with the same value as its default — harmless, but shows the script was written against a slightly different flow.
