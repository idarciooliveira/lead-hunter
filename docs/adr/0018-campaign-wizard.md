# 0018. Create campaigns with an interactive wizard; YAML stays the stored format

- Date: 2026-09-28
- Status: Accepted
- Amends: [0009](0009-cli-first.md), [0013](0013-campaigns-as-yaml.md)

## Context

ADR 0013 made a YAML file the only way to create a campaign. In practice the owner wants to start a campaign by answering the questions, not by editing YAML by hand. ADR 0009 said every command is non-interactive, so that Railway can run commands as scheduled jobs.

## Decision

- `campaign new` asks the 10 onboarding questions and the main search settings in the terminal: terms, locations, places per search, target keywords, excluded names. Defaults cover everything except the name, offer, buyers, and search terms.
- Excluded names default to the reference clients, so current clients never show up as leads. Target keywords default to the search terms.
- The wizard saves the campaign to the database and writes `campaigns/<slug>.yml`, so campaigns still live in git as ADR 0013 intended. `--no-file` skips the file, and `--dir` changes the folder.
- Before replacing an existing campaign or file, the wizard asks. The default answer is no.
- `campaign create -f` stays for editing and re-saving files.
- `campaign new` is the only interactive command. Everything a scheduled job needs stays non-interactive, as ADR 0009 requires.

## Consequences

Starting a campaign takes a couple of minutes in the terminal. Fine-grained settings such as `excludeKeywords` and `qualifyShare` are still edited in the file. In Docker the wizard needs an interactive terminal, which `docker compose run` provides by default. On Linux, files written from the container belong to root.
