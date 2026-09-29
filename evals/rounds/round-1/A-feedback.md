# Agent A — Feedback to PO, Round 1

Prioritized improvement asks from the round-1 eval run (`bash evals/harness.sh round-1`, all four tests exit 0). Evidence quotes come from `evals/runs/round-1-*.txt`. I judge the experience; you decide what and how to fix.

## High priority

### 1. Raw `null` shown to the user in the company update summary
- **Evidence:** `evals/runs/round-1-company-update.txt` — `[null new clients, 500000 Kz]` (C10 target summary).
- **What to improve:** Internal unset values must never reach the terminal. A PME owner has no way to interpret `null` and will assume something broke.
- **Suggested direction:** Render unset numbers as `—` or `não definido`, or omit the missing half of the summary entirely (`[500000 Kz de receita]`).

### 2. Overwrite confirm shows `[y/N]` but Enter overwrites
- **Evidence:** `evals/runs/round-1-company-update.txt` — `campaigns/company.yml already exists. Overwrite it? [y/N] > Saved the company profile for TchiowaLabs.` The scripted user only sent blank lines; the file was still overwritten.
- **What to improve:** The displayed default (`N`) and the honoured default (`Y`) disagree, on the one prompt where a wrong Enter destroys a hand-edited file.
- **Suggested direction:** Make the bracketed default the real default, or require an explicit `y` for destructive confirms. Mismatch either way is unacceptable on an overwrite prompt.

### 3. End-of-wizard guidance contradicts itself
- **Evidence:** `evals/runs/round-1-campaign-new.txt` — `Saved campaign 'clinicas-dentarias-em-luanda'.` followed by `Edit it for finer settings, then: campaign create -f campaigns/clinicas-dentarias-em-luanda.yml`. Same pattern in `round-1-company-create.txt` (`Edit it, then: company update -f campaigns/company.yml`). The dry-run succeeds with no `campaign create` in between, so the "then:" step is not needed.
- **What to improve:** "Saved" + "then run create" reads as two contradictory statements about whether the campaign exists. A first-time user will either re-run create redundantly or distrust the wizard.
- **Suggested direction:** Pick one story: either "saved; edit the yml and re-import to apply changes" or "drafted; run create to activate". State which file is the source of truth.

## Medium priority

### 4. Campaign wizard numbering (1/11..11/11) does not match the actual flow
- **Evidence:** `evals/runs/round-1-campaign-new.txt` — the goal block (`Meetings [3]`, `Won clients [1]`, `End date`, `Leads per week`, N/M stop rule) and the whole `— Search` section are unnumbered, and `11/11 Tone of the messages` appears after the goal block.
- **What to improve:** Progress numbering that skips six-plus prompts is worse than no numbering.
- **Suggested direction:** Either number every prompt or number only the 11 "big" questions and label the rest as subsections of the nearest number.

### 5. "Search terms" vs "Keywords that mark a target sector" overlap is unexplained
- **Evidence:** `evals/runs/round-1-campaign-new.txt` — terms answered `clínica`, `clínica dentária`, then the keyword prompt appears pre-filled with the same words `[clínica; clínica dentária]`. No prompt explains how the two fields differ or interact.
- **What to improve:** Two near-identical free-text fields with no visible distinction will get filled with the same thing or skipped blindly.
- **Suggested direction:** One sentence on the division of labour (e.g. terms = what we type into Google Maps; keywords = which results count as the target sector), or merge them with an opt-out.

### 6. Mixed-language prompts for a Portuguese-first persona
- **Evidence:** throughout `round-1-campaign-new.txt` — instructions in English (`Which service do you pitch?`, `Who qualifies`), examples and defaults in Portuguese (`e.g. clínicas dentárias`, `[Formal, em português...]`).
- **What to improve:** The load-bearing question text is in the persona's weaker language while decoration is in their stronger one — inverted from what helps.
- **Suggested direction:** A `--lang pt` flag or full PT prompt set; the examples already prove the vocabulary exists.

### 7. Scripted-user misalignment in `company-update` objections (report, unfixed)
- **Evidence:** `evals/scripts/company-update.txt` lines 16-18 land on the wrong canned objection prompts in `round-1-company-update.txt` (sobrinho answer consumed at `"Já tenho Instagram ou Facebook" >`). The harness blank-line buffer converts this into a silent pass with wrong stored data.
- **What to improve:** The eval now passes while storing objection→answer pairs the simulated user never wrote; future rounds will eval against a corrupted profile.
- **Suggested direction:** Realign the script's objection answers to the current canned objection texts in a round-2 change (PO/agent B owns scripts; I did not edit them per eval rules).

## Low priority

### 8. Prompts run inline after the previous input marker
- **Evidence:** `evals/runs/round-1-company-create.txt` — `> C2/10  How do you introduce...` (question starts on the same line as the previous `> `).
- **What to improve:** Scannability in the live terminal; each new question should start on a fresh line.
- **Suggested direction:** Emit a newline before each prompt after the first.

### 9. C7 (cases) asks "Add a case?" before showing what a case is
- **Evidence:** `evals/runs/round-1-company-create.txt` — `Results you can prove. The pitch only ever claims these. Up to 2 now.` then immediately `Add a case? [y/N]`.
- **What to improve:** The user commits to adding a case before seeing its fields (sector, client, problem, result-with-number).
- **Suggested direction:** Show the field list or one worked example before the y/N gate, or move the gate inside.

### 10. Dry-run ends without a "what next"
- **Evidence:** `evals/runs/round-1-campaign-run-dry.txt` — ends at `Up to 80 places, about $0.32 at the configured price per place.`
- **What to improve:** The rest of the wizard always ends with a `Next:` line; the dry-run does not say how to go live.
- **Suggested direction:** Append `Next: run without --dry-run to start this campaign (will spend Apify credit)`.

## Handoff to Agent B: top themes are honesty of destructive defaults (null leak, overwrite prompt), contradictory end-of-wizard next-step guidance, and the campaign wizard's broken progress numbering / search-term duplication.
