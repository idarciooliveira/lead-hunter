# CLI Eval Loop

A 4-agent improvement loop over the CLI wizards (`company setup`, `campaign new`) with two cycles per run.

```
Agent A (eval) ──feedback──▶ Agent B (PO) ──instructions──▶ Agent C (dev) ──change log──▶ Agent D (QA)
      ▲                                                                           │
      └──────────────────── next cycle (A re-evals the new build) ◀── PASS ───────┘
                                                      BLOCKED ──▶ back to C (one loop)
```

Agent definitions: `.opencode/agent/eval-{a-dex,b-po,c-java,d-qa}.md`. The orchestrator launches each
agent in a fresh session and tells it only which files to read, so every round starts with clean context.

## Layout

- `harness.sh` — deterministic eval harness. Builds the `lead-hunter:eval` Docker image, runs the CLI
  against scratch DB `leadhunter_eval` on the compose network, from a scratch working directory, with
  scripted stdin. Usage: `bash evals/harness.sh <tag> [company|campaign|run|all]`.
- `scripts/*.txt` — the simulated user's answers, one line per prompt (persona: TchiowaLabs).
- `runs/<tag>-<test>.txt` — transcripts; the agents' evidence.
- `work/<tag>/` — scratch cwd per harness run (gitignored).
- `rounds/round-<N>/` — agent handoffs: `A-scorecard.md`, `A-feedback.md`, `B-instructions.md`,
  `C-change-log.md`, `D-qa-verdict.md`. Final overview: `rounds/OVERVIEW.md`.

## Notes

- The repo's `mvnw` has CRLF endings and this host has no javac, so builds/tests run in Docker
  (`maven:3.9-eclipse-temurin-21`); see the agent definitions for exact commands.
- `campaign run` is only ever executed with `--dry-run` — evals never spend Apify money.
- Wizard question changes may shift the scripted-answer alignment; the harness appends a blank-line
  buffer so trailing confirms still default, but mid-wizard shifts show up as refused inputs in the
  transcripts (the evaluator is told to report, not fix).
