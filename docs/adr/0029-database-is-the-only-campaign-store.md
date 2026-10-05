# 0029. Store campaigns and the company profile only in the database

- Date: 2026-10-04
- Status: Accepted
- Supersedes: [0013](0013-campaigns-as-yaml.md)
- Amends: [0018](0018-campaign-wizard.md), [0019](0019-company-profile.md)

## Context

ADR 0013 made a YAML file the format for campaigns, and ADR 0018 and 0019 made the wizards write `campaigns/<slug>.yml` and `campaigns/company.yml` next to the database row. The database was already the source of truth: runs, scoring and `campaign show` read the `campaign` and `company` tables, and a file edit did nothing until someone re-imported it. The file was a second copy that could go stale.

A web client is coming (ADR 0030). It will create and edit campaigns and the company profile through an API, which makes a third writer and a worse drift problem. The reason for 0013, keeping targeting history in git, was never used in practice.

## Decision

- Postgres is the only store for campaigns and the company profile.
- `campaign new` and `company setup` save to the database and write no file. The `--no-file`, `--dir` and `--file` options on them are removed.
- `campaign create -f` and `company update -f` stay as import commands, for seeding a fresh database and for bulk edits. `campaign template` and `company template` stay as documentation of the questions.
- Export from the database to YAML is not automatic. If we want backups or history, it becomes an explicit command.
- The existing `campaigns/*.yml` files are kept as seed examples until their content is confirmed in the database, then removed.
- Validation (`CampaignFileParser.validate`, `CampaignChecks`, `CompanyProfileParser.validate`) stays in the domain code and is shared by the CLI and the API.

## Consequences

One copy of each campaign, so nothing to reconcile. Changes to targeting lose their git history. We accept that, and a database backup or an export command covers the need. Deploying to Railway no longer depends on files on disk. The overwrite prompts for files disappear from the wizards.
