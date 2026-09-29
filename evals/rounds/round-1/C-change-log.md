# Agent C — Change Log, Round 1

Implementing `evals/rounds/round-1/B-instructions.md` (directives 1–8).

## Per-directive status

### 1. Fix the overwrite default mismatch — DONE
- **Changed**: `Prompter.java` — new `confirmOverwrite(String)` next to `confirm()`: only a line
  starting with `y` overwrites; any other input (empty, `n`, anything not starting with `y`) prints
  `Cancelado — nada foi gravado.` and returns false. The confirm text still renders as
  `… already exists. Overwrite it? [y/N]`.
- **Changed**: `CompanyCommand.Setup.run()` — the overwrite confirm now gates the whole save. On a
  non-`y` answer it prints `Cancelado — nada foi gravado.` and returns before `company.save(...)`,
  so neither the DB row nor the yml is written and no `Saved the company profile` line appears.
- **Acceptance**: blank answer → `Cancelado — nada foi gravado.` + existing yml byte-identical;
  explicit `y` → `Saved the company profile` + write. **Met** — see "Supplementary evidence" below:
  `evals/runs/round-1-c-company-cancel.txt` shows blank → Cancelado with sha256-identical yml
  (`campaigns/company.yml: OK`).
- **Note**: B's sheet places this confirm in `CompanyWizard.java`; it actually lives in
  `CompanyCommand.Setup` (the wizard finishes before the confirm is asked). Implemented at the real
  call site.

### 2. Never show a raw `null` to the user — DONE
- **Changed**: `CompanyWizard.askTarget()` — the C10 summary note renders unset components as `—`
  (`[— clientes novos, 500000 Kz]` / `[20 clientes novos, — Kz]`); when both components are unset
  the note is omitted entirely. Also localized `new clients` → `clientes novos` per B's exact
  expected output.
- **Acceptance**: no `null` in the update flow output; exact string `[— clientes novos, 500000 Kz]`
  with the round-1 profile. **Met** — `evals/runs/round-1-c-company-update.txt` line 45 shows
  `[— clientes novos, 500000 Kz]`; `grep -E null evals/runs/round-1-c-*.txt` finds nothing.

### 3. Tell one story at the end of each wizard — DONE
- **Changed**: `CompanyCommand.Setup` — the `Wrote … Edit it, then: company update -f …` line is
  replaced by `Guardado em campaigns/company.yml. Edita-o e grava as alterações com: company update -f campaigns/company.yml`
  (printed with the actual `--file` path; identical to B's exact text for the default path). The
  `Saved the company profile …` line and `Next: campaign new` are kept.
- **Changed**: `CampaignCommand.New` — the `Wrote … Edit it for finer settings, then: campaign create -f …`
  line is replaced by `Guardado em campaigns/<slug>.yml. Testa sem gastar: campaign run <slug> --dry-run`.
  The `Saved campaign '…'` and `Next: campaign run … --dry-run` lines are kept.
- **Acceptance**: no output line instructs the user to run `campaign create` before dry-running; a
  dry-run works immediately after `campaign new` (harness runs them back-to-back, exit 0). **Met**.
- **Note**: as with directive 1, these strings live in the command classes, not the wizard classes
  named by B.

### 4. Number or label every campaign prompt — DONE
- **Changed**: `CampaignWizard.run()` — the goal block (`askGoal()`) moved after `11/11 Tone`, opened
  by the header `— Metas de campanha —`; the search section opens with `— Procura no Google Maps —`
  (both printed exactly as specified, replacing the old `— The goal` / `— Search` section lines and
  the redundant `Goal for this campaign` line inside `askGoal`). The numbered sequence `1/11..11/11`
  now runs uninterrupted; `11/11` is the last numbered prompt; the goal and search prompts carry no
  `N/11` ids and sit under their headers.
- **Acceptance**: every campaign prompt carries an `N/11` id (or belongs to one, e.g. the signal
  prompts under `6/11` and the `Number` prompt under `3/11`) or sits under an explicit
  `— <Section> —` header; `11/11` is last numbered. **Met** — `round-1-c-campaign-new.txt`.

### 5. Explain "search terms" vs "keywords" — DONE
- **Changed**: `CampaignWizard.run()` — the search section prints once, before the first search
  prompt: `A "search term" é o que digitamos no Google Maps. As "keywords" filtram que resultados
  contam como alvo do setor.` The `Google Maps search terms` prompt text is unchanged. The keyword
  prompt is now one line: `Keywords that mark a target sector — matched as whole words in the name
  or category. Defaults to the search terms.` (the `;`-joined default `[clínica; clínica dentária]`
  behaviour is unchanged — still taken from the terms).
- **Acceptance**: intro sentence appears exactly once before the first search prompt; keyword prompt
  contains `Defaults to the search terms.` in one line. **Met** — `round-1-c-campaign-new.txt`
  lines 52–61.

### 6. Show what a case is before the C7 y/N gate — DONE
- **Changed**: `CompanyWizard.askCases()` — before the `Add a case?` confirm, one note line:
  `Um caso mostra o que já consegues: setor, cliente, problema, resultado com número — e.g. clínicas
  dentárias, Clínica X, marcava só por ligação, +40 marcações em 3 meses.`
- **Acceptance**: the prompt is preceded by a single line naming setor, cliente, problema, resultado
  in order with one concrete figure. **Met** — appears in both create and update transcripts.

### 7. Dry-run gets a `Next:` line — DONE
- **Changed**: `CampaignCommand.Run` — after the `Up to N places, about $…` line:
  `Next: run without --dry-run to start this campaign (will spend Apify credit)` (English, matching
  the surrounding `Next:` lines' language, verbatim from B's sheet).
- **Acceptance**: dry-run output ends with the line and contains `Apify credit`. **Met** —
  `round-1-c-campaign-run-dry.txt` line 4.

### 8. Realign the company-update script's objection answers — DONE
- **Changed**: `evals/scripts/company-update.txt`, C8 block (lines 15–18):
  - line 15 unchanged: the price reply answers `É caro`;
  - line 16 **replaced**: it stored the objection phrasing `Já tenho um sobrinho que faz sites` in
    the `Já tenho Instagram ou Facebook` slot; now a distinct Instagram reply answers it:
    `Tenho, mas o Instagram não procura clientes por mim`;
  - line 17 unchanged: `O sobrinho nao tem os casos que temos` answers the sobrinho objection —
    verified against the canned order in `CompanyWizard.COMMON_OBJECTIONS` (`É caro`,
    `Já tenho Instagram ou Facebook`, `O meu sobrinho faz isso`, `Agora não tenho tempo`), it
    already lands on the right prompt;
  - line 18 unchanged: `Quando o negocio parar de andar, a gente fala` answers
    `Agora não tenho tempo` — already correct.
- **One extra eval-integrity line** (recorded as a deviation, see below): line 22 (blank) became
  `y` so the update run confirms the overwrite and the corrected mapping actually reaches the saved
  `campaigns/company.yml`. Line count (25) and otherwise blank rhythm unchanged.
- **Acceptance**: after a fresh `company-update` run the saved yml maps each answer to the
  objection it responds to. **Met** — `evals/work/round-1-c/campaigns/company.yml` now contains
  `É caro → Temos o pacote basic…`, `Já tenho Instagram ou Facebook → Tenho, mas o Instagram não
  procura clientes por mim`, `O meu sobrinho faz isso → O sobrinho nao tem os casos que temos`,
  `Agora não tenho tempo → Quando o negocio parar de andar, a gente fala`. Spot-check: the sobrinho
  reply is attached to the canned sobrinho objection — note the canned text in code is
  `O meu sobrinho faz isso`, not `Já tenho um sobrinho que faz sites` as quoted in B's sheet;
  the mapping is semantically correct against the real prompt order (verified, no wizard wording
  change).

### Realigned `evals/scripts/campaign-new.txt` (authorized by the orchestrator)
- Moved the goal answers (`3`, `1`, blank end-date, blank leads/week, `2`, blank M) from before the
  tone line to after it, matching directive 4's new question order. Persona answers unchanged:
  sector `clínicas dentárias`, problem and why-now as before, signals `w,w,d,w`, min reviews 3,
  meetings 3, wins 1, N=2, terms `clínica` + `clínica dentária`, exclude `agência digital`.
- Wizard tests treated the same way: `CampaignWizardTest.ANSWERS` reordered identically (goal block
  after the tone line), values unchanged.

## Build/test results
- Host has no JDK; all builds in Docker (`maven:3.9-eclipse-temurin-21`).
- `docker run … mvn -q test` — **117 tests, 1 failure**: pre-existing and unrelated to this round
  (`CompanyProfileParserTest.theCompanyFileInTheRepoIsValid` expects clients
  `ajabalg/kintexia/horizontetourangola` in `campaigns/company.yml`, but the drifted content was
  already committed in d5d40f3). Confirmed pre-existing by running the same test on a pristine
  `git worktree` of HEAD: identical failure. All wizard/CLI tests pass, including the reordered
  `CampaignWizardTest.ANSWERS`.
- One intermediate compile error (duplicated pitch block during the CampaignWizard edit) was fixed
  before this final result.

## Harness results (`bash evals/harness.sh round-1-c`)
```
test: company
  company-create: exit=0 -> evals/runs/round-1-c-company-create.txt
  company-update: exit=0 -> evals/runs/round-1-c-company-update.txt
test: campaign
  campaign-new: exit=0 -> evals/runs/round-1-c-campaign-new.txt
  campaign-run-dry: exit=0 -> evals/runs/round-1-c-campaign-run-dry.txt
harness done: all tests exited 0.
```
Transcripts read and verified for every touched question; `campaign run` only ever ran with
`--dry-run`; no real external APIs were called.

## Supplementary evidence for directive 1 (Cancelado path)
The update script now explicitly answers `y` (directive 8 needs the corrected mapping in the saved
yml), so the harness transcript shows the overwrite path. The blank→Cancelado path is demonstrated
separately in `evals/runs/round-1-c-company-cancel.txt`: an all-Enter `company setup` run reaches
`Overwrite it? [y/N]`, answers blank, prints `Cancelado — nada foi gravado.`, and the existing
`campaigns/company.yml` is sha256-identical before/after.

## Deviations from B's instructions
1. **File locations** (directives 1, 3): B names `CompanyWizard.java` / `CampaignWizard.java`, but
   the overwrite confirm and the end-of-wizard `Saved`/`Edit it` lines live in
   `CompanyCommand.Setup` / `CampaignCommand.New`. Implemented where the strings actually are.
2. **company-update.txt line 22: blank → `y`** (directive 8): B's "keep blank-line rhythm unchanged
   otherwise" collides with B's own acceptance criteria — directive 1 makes any blank at the
   overwrite confirm abort the whole save (`Cancelado`), which would leave `campaigns/company.yml`
   byte-identical to the create-run version, making directive 8's saved-yml check unverifiable.
   Chose the reading that satisfies both criteria: product behaviour per directive 1 (verified
   separately, see above), and an explicit `y` in the eval script so the corrected objection
   mapping lands in the yml.
3. **Directive 8 scope**: B quotes the canned objection as `Já tenho um sobrinho que faz sites`;
   the code's canned text is `O meu sobrinho faz isso`. Per the directive's own "prompt order
   verification — no wizard wording change", answers were mapped against the real prompt order.
4. **Directive 2 wording**: the summary note's `new clients` became `clientes novos` because B's
   acceptance specifies the exact output `[— clientes novos, 500000 Kz]`.

Handoff to Agent D (QA): directives 1–8 landed — verify no `null` in any transcript, Cancelado
evidence in `round-1-c-company-cancel.txt`, one coherent end-of-wizard story, `— Metas de campanha —`
/ `— Procura no Google Maps —` sections, dry-run `Next:` line, and the corrected objection mapping
in `evals/work/round-1-c/campaigns/company.yml`.
