# Agent D — QA Verdict, Round 1

## PASS

All three verification layers were re-run by me from the current working tree (not taken from C's
change log), plus the two supplementary scripts/transcripts C flagged. Everything C claimed checks
out; the only failures observed are pre-existing on pristine HEAD and are excluded per instructions.

## Layer 1 — Unit tests (`mvn -q test`, Docker `maven:3.9-eclipse-temurin-21`)

- `Tests run: 117, Failures: 1, Errors: 0` — the single failure is
  `CompanyProfileParserTest.theCompanyFileInTheValid` (expects clients
  `ajabalg/kintexia/horizontetourangola` in `campaigns/company.yml`).
- Pre-existing check: I ran the same test class in a fresh `git worktree` at pristine HEAD
  (commit 53bca5a, which excludes C's uncommitted changes) — identical failure. Not counted
  against C, per instructions.
- All wizard/CLI tests pass, including the reordered `CampaignWizardTest.ANSWERS`
  (goal block now after the `11 tone` line — `CampaignWizardTest.java:46-49`).

## Layer 2 — Integration tests (scratch Postgres `leadhunter_qa`, reset schema, full suite)

- `Tests run: 117, Failures: 2` —
  1. `CompanyProfileParserTest.theCompanyFileInTheRepoIsValid` (same pre-existing drift).
  2. `CliIntegrationTest.createsAndPlansTheExampleCampaign:113` — `campaign create` exits 1.
- Pre-existing check: I re-ran `CliIntegrationTest` alone in a fresh worktree at pristine HEAD on a
  clean scratch DB — same failure, same line (`expected: 0 but was: 1`). The test loads the drifted
  `campaigns/company.yml` via `company update` and then creates `clinicas-luanda.yml`, which pitches
  a service the drifted profile no longer sells. Both failures share the d5d40f3 drift root cause
  and are excluded per instructions.
- Everything else, including `CampaignWizardTest` (6), `CompanyWizardTest` (2),
  `CampaignRunnerIntegrationTest` (8), `UsageRepositoryIntegrationTest` (6): pass.

## Layer 3 — Fresh eval harness (`bash evals/harness.sh round-1-qa`, image rebuilt from current code)

```
company-create: exit=0 -> evals/runs/round-1-qa-company-create.txt
company-update: exit=0 -> evals/runs/round-1-qa-company-update.txt
campaign-new:   exit=0 -> evals/runs/round-1-qa-campaign-new.txt
campaign-run-dry: exit=0 -> evals/runs/round-1-qa-campaign-run-dry.txt
harness done: all tests exited 0.
```
No wizard-answer misalignment: `grep -c "Required."` is 0 in all four transcripts; every script
line landed on exactly one prompt (traced the full campaign-new run prompt-by-prompt).

## Layer 4 — Acceptance criteria (B's instructions, verified against round-1-qa transcripts)

1. **Overwrite default** — update transcript ends `Overwrite it? [y/N] > Saved the company profile`
   (explicit `y` from the script). The blank→cancel path I reproduced independently: ran
   `company setup` all-Enter against an existing yml in a scratch dir → prints
   `Cancelado — nada foi gravado.`, no `Saved` line, exit 0, and the existing
   `campaigns/company.yml` sha256 (`b05184e0…`) identical before/after. Matches C's supplementary
   transcript `evals/runs/round-1-c-company-cancel.txt:48`. `Prompter.confirmOverwrite`
   (Prompter.java:149) only accepts a leading `y`.
2. **No raw `null`** — `grep -rn "null"` over `round-1-qa-*` and `round-1-c-*` transcripts: no
   matches. Update transcript line 45 shows exactly `[— clientes novos, 500000 Kz]`
   (CompanyWizard.java:153, `orDash`).
3. **One end-of-wizard story** — company: `Guardado em campaigns/company.yml. Edita-o e grava as
   alterações com: company update -f campaigns/company.yml` + `Next: campaign new`; campaign:
   `Guardado em campaigns/<slug>.yml. Testa sem gastar: campaign run <slug> --dry-run` +
   `Next: campaign run … --dry-run`. Grep for `Edit it|campaign create|Wrote` over all transcripts:
   no matches. Dry-run works immediately after `campaign new` (harness runs them back-to-back, exit 0).
4. **Numbered/labelled campaign prompts** — transcript shows `1/11`…`11/11` uninterrupted
   (11/11 Tone is the last numbered prompt), then `— Metas de campanha —` (once, line 40) with the
   goal block under it, then `— Procura no Google Maps —` (once, line 52) with the search prompts
   under it. Note: the two preamble prompts (`Campaign name`, `Short id`) remain unnumbered and
   header-less — that is the pre-round-1 preamble, outside the directive's change scope ("nothing
   else is renumbered"); not a regression.
5. **Search terms vs keywords** — intro sentence appears exactly once before the first search
   prompt (line 53); keyword prompt is one line containing
   `Defaults to the search terms.` (line 60) with the `;`-joined default intact.
6. **Case explanation** — the single line `Um caso mostra o que já consegues: setor, cliente,
   problema, resultado com número — e.g. … +40 marcações em 3 meses.` precedes `Add a case? [y/N]`
   in both create (line 28) and update (line 33) transcripts.
7. **Dry-run `Next:` line** — transcript ends `Next: run without --dry-run to start this campaign
   (will spend Apify credit)` (English, matching the surrounding `Next:` lines).
8. **Objection mapping** — saved `evals/work/round-1-qa/campaigns/company.yml:26-33` maps exactly:
   `É caro → Temos o pacote basic…`, `Já tenho Instagram ou Facebook → Tenho, mas o Instagram não
   procura clientes por mim`, `O meu sobrinho faz isso → O sobrinho nao tem os casos que temos`,
   `Agora não tenho tempo → Quando o negocio parar de andar, a gente fala` — matching
   `company-update.txt` lines 15-18 and the real canned order in `CompanyWizard.COMMON_OBJECTIONS`
   (`CompanyWizard.java:22-26`, sobrinho objection is `O meu sobrinho faz isso`; the answer lands on
   it correctly). The script's line-22 `y` deviation is sound: with directive 1, a blank there would
   abort the save and leave directive 8's saved-yml check unverifiable.

Script edits verified: `evals/scripts/campaign-new.txt` goal answers (lines 18-23) now land on the
`— Metas de campanha —` prompts after tone; persona answers unchanged; no repeated/refused inputs.

## Notes (not blockers)

- B's directive 1/3 name `CompanyWizard.java` / `CampaignWizard.java`; the strings actually live in
  `CompanyCommand.Setup` / `CampaignCommand.New` / `Prompter`. C implemented at the real call sites
  — correct behaviour, location deviation is legitimate.
- Suggestion for a later round: after fixing the drifted `campaigns/company.yml` (pre-existing),
  `CliIntegrationTest.createsAndPlansTheExampleCampaign` should go green again — no product change
  needed for it.

Round 1 complete
