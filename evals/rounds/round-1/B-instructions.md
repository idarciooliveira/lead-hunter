# Agent B — Instructions for the Java developer agent, Round 1

Context: A's first eval of the wizards found solid question quality (every prompt has a Portuguese example and default), but the round's failures are all about **honesty**: the overwrite confirm shows a default it does not honour, a raw `null` reaches the non-technical user, and "Saved" + "then run create" tell a contradictory story about whether the thing the user just built exists. A second theme is **orientation**: campaign numbering that skips prompts and two search fields that look identical. These 8 directives fix that, highest value first.

## Directives

### 1. Fix the overwrite default mismatch
- **Change**: The overwrite confirm must require an explicit `y` to overwrite. Any other input (empty, `n`, anything not starting with `y`) prints `Cancelado — nada foi gravado.` and returns to the previous flow without writing. The confirm text stays as-is: `campaigns/company.yml already exists. Overwrite it? [y/N]`, so the bracketed default (`N`) is now the honoured default.
- **Where**: `CompanyWizard.java`, overwrite confirm (end of the update flow, before writing the yml).
- **Why**: A's high-priority item 2: display said `[y/N]` but a bare Enter overwrote the file — the one prompt where a wrong Enter destroys a hand-edited file (A's feedback §2, `round-1-company-update.txt`).
- **Acceptance criterion**: In the update transcript, blank-line answers to the overwrite confirm are followed by `Cancelado — nada foi gravado.` and the existing yml is byte-identical to before; only an explicit `y` line produces the `Saved the company profile` line.

### 2. Never show a raw `null` to the user
- **Change**: In the C10 target summary, render an unset component as `—`. With only `revenueKz: 500000` set, the exact output is `[— clientes novos, 500000 Kz]`; with only `newClients` set it is `[20 clientes novos, — Kz]`; with nothing set, omit the summary line entirely.
- **Where**: `CompanyWizard.java`, C10 target summary line (update flow).
- **Why**: A's high-priority item 1: `[null new clients, 500000 Kz]` shown to a PME owner who cannot interpret `null` (A's feedback §1, `round-1-company-update.txt`).
- **Acceptance criterion**: The string `null` does not appear anywhere in `evals/runs` output for the update flow; with the round-1 profile (`revenueKz` set, `newClients` absent) the summary reads exactly `[— clientes novos, 500000 Kz]`.

### 3. Tell one story at the end of each wizard
- **Change**: Replace the contradictory `Edit it ... then:` line with one cohesive pattern. After saving, print exactly:
  - `CompanyWizard` (create/update): `Guardado em campaigns/company.yml. Edita-o e grava as alterações com: company update -f campaigns/company.yml`
  - `CampaignWizard` (new): `Guardado em campaigns/<slug>.yml. Testa sem gastar: campaign run <slug> --dry-run`
  Drop `Edit it for finer settings, then: campaign create -f campaigns/...yml` and `Edit it, then: company update -f campaigns/company.yml` entirely — the dry-run works with no `campaign create` in between, so that instruction is false. Keep the existing `Saved ...` line as-is; the new line replaces only the `Edit it ...` line.
- **Where**: `CompanyWizard.java` and `CampaignWizard.java`, final `Next:` line after saving.
- **Why**: A's high-priority item 3: "Saved" + "then create" reads as contradictory since the dry-run works with no `campaign create` in between (A's feedback §3, both transcripts).
- **Acceptance criterion**: The ending never mentions a command whose absence does not break the next step; the dry-run completes immediately after a fresh `campaign new` with no intermediate command, and no output line instructs the user to run `campaign create` before dry-running.

### 4. Number or label every campaign prompt (goal block + search section)
- **Change**: Keep the 11 main question ids `1/11..11/11`, and put every currently unnumbered prompt under an explicit section header:
  - Move the goal block (Meetings / Won clients / End date / leads-week / N-M stop rule) so it sits **after** `11/11 Tone`, and open it with a single header line: `— Metas de campanha —`.
  - Open the search section with a single header line: `— Procura no Google Maps —`.
  - Under each header the prompts keep their own labels but get no `N/11` ids; nothing else is renumbered.
  Result: the numbered sequence `1/11..11/11` runs with no unnumbered prompt in between, and the two trailing sections are clearly marked.
- **Where**: `CampaignWizard.java`, goal block (currently between 10/11 and 11/11) and the `— Search` section.
- **Why**: A's medium-priority item 4: the wizard promises `1/11..11/11` while 13 prompts run unnumbered and 11/11 renders after the goal block, so progress looks broken (A's feedback §4, scorecard rows `goal block` and `search: terms` in `round-1-campaign-new.txt`).
- **Acceptance criterion**: Every prompt in the campaign transcript either carries an `N/11` id or sits under an explicit `— <Section> —` header line; `11/11` is the last numbered prompt; the goal block and the search prompts each have a header line printed once before them.

### 5. Explain "search terms" vs "keywords that mark a target sector"
- **Change**: Reword the search-section prompts so the division of labour appears once:
  - First, at the top of the search section (before the first search prompt), print once: `A "search term" é o que digitamos no Google Maps. As "keywords" filtram que resultados contam como alvo do setor.`
  - The `search terms` prompt keeps its text: `Google Maps search terms — e.g. clínica, clínica dentária`.
  - The keyword prompt becomes: `Keywords that mark a target sector — matched as whole words in the name or category. Defaults to the search terms. [clínica; clínica dentária]` (keep the `;`-joined default behaviour).
- **Where**: `CampaignWizard.java`, search section headers/prompts (`search: terms` and `search: keywords that mark a target sector`).
- **Why**: A's medium-priority item 5: two near-identical free-text fields get filled with the same thing or skipped blindly (A's feedback §5, scorecard row `search: terms`).
- **Acceptance criterion**: The section intro sentence appears exactly once before the first search prompt, and the keyword prompt text contains the sentence `Defaults to the search terms.` in one line.

### 6. Show what a case is before the C7 y/N gate
- **Change**: Insert before `Add a case? [y/N]` one worked example line in Portuguese: `Um caso mostra o que já consegues: setor, cliente, problema, resultado com número — e.g. clínicas dentárias, Clínica X, marcava só por ligação, +40 marcações em 3 meses.` (One line; the y/N prompt and the field flow stay unchanged.)
- **Where**: `CompanyWizard.java`, C7 (provable results / cases).
- **Why**: A's low-priority item 9: the user commits to adding a case before seeing its fields (A's feedback §9, `round-1-company-create.txt`).
- **Acceptance criterion**: The `Add a case?` prompt is preceded in the same transcript segment by a single line containing the four field names in order (setor, cliente, problema, resultado) with one concrete figure.

### 7. Dry-run gets a `Next:` line
- **Change**: After the cost-estimate line (`Up to 80 places, about $0.32 ...`), append exactly: `Next: run without --dry-run to start this campaign (will spend Apify credit)`.
- **Where**: `CampaignWizard.java` (or the dry-run print section of the campaign run command — wherever the `Up to N places` line is emitted), dry-run output.
- **Why**: A's low-priority item 10: every wizard ends with a `Next:` line; the dry-run does not say how to go live (A's feedback §10, `round-1-campaign-run-dry.txt`).
- **Acceptance criterion**: The dry-run transcript ends with the `Next: run without --dry-run ...` line matching the wizard's existing `Next:` tone, and it contains the words `Apify credit` (or their Portuguese equivalent if the surrounding `Next:` lines use Portuguese — keep one language).

### 8. Realign the company-update script's objection answers (eval integrity, not product)
- **Change**: In `evals/scripts/company-update.txt`, reorder/replace only the C8 input block so each answer lands on its matching canned objection prompt: the sobrinho reply answers `"Já tenho um sobrinho que faz sites"`, the tempo reply answers `"Agora não tenho tempo"`, and a distinct Instagram reply answers `"Já tenho Instagram ou Facebook"`. Keep line count and blank-line rhythm unchanged otherwise.
- **Where**: `evals/scripts/company-update.txt`, objection-answers block (lines 16-18 region of the current script) + the matching prompt order in `CompanyWizard.java` C8 canned objections. (Script +, at most, prompt order verification — no wizard wording change.)
- **Why**: A's medium-priority item 7: the script currently stores objection→answer pairs the simulated user never wrote, so future rounds evaluate against a corrupted profile (A's feedback §7, scorecard update table C8 row).
- **Acceptance criterion**: After a fresh `company-update` run, the saved `campaigns/company.yml` objection list maps exactly the answers written in the updated script to the objections whose texts they respond to (spot-check: the sobrinho reply is attached to `Já tenho um sobrinho que faz sites`).

## Rejected from A's feedback

- **Item 6 (mixed-language prompts / `--lang pt`)** — rejected this round. A new CLI flag and a full PT prompt set is a product-scope change (needs an ADR per `docs/adr/README.md`), not a wizard-wording fix; defer to round 2 and propose the flag there if the ask survives a second look.
- **Item 8, prompt spacing / newline before each prompt** — rejected as evaluator presentation taste: it changes transcript cosmetics, not user value; the live terminal already prints prompts as separate lines in normal interactive use. Revisit only if a real user complains about terminal readability.

## Handoff to Agent C

Handoff to Agent C: verify directives 1-8 after they land — no `null` in any output, Enter on overwrite cancels, one coherent end-of-wizard story, goal/search sections labelled, company.yml objection mapping intact.
