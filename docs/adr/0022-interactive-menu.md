# 0022. Open an interactive menu when the tool starts in a terminal

- Date: 2026-09-29
- Status: Accepted
- Amends: [0009](0009-cli-first.md), [0018](0018-campaign-wizard.md)

## Context

Daily use means typing a full command each time: the subcommand, the campaign slug, the flags. Each command also starts the JVM and Spring again. Running a campaign, reading its leads and opening one lead takes three launches and three slugs.

ADR 0018 made `campaign new` the only interactive command. Scheduled jobs on Railway still need every other command to run without a terminal.

## Decision

- `lead-hunter` with no arguments opens a numbered menu when stdin and stdout are a terminal. Otherwise it prints the help text as before. `lead-hunter menu` opens the menu directly.
- The menu has five entries: run a campaign, browse leads, new campaign, company profile, usage and costs. Enter or 0 goes back or quits.
- Campaigns are picked from a list, so no slug is typed.
- Every entry runs an existing command through the same picocli root. The menu holds no business logic, and errors print as `error: <message>` and return to the menu.
- Running a campaign shows the dry run first (searches and maximum cost) and asks `[y/N]` before the real run.
- Numbered choices only. No new dependency.

## Consequences

One session covers run, browse and open, with one JVM start. Commands with flags keep working for scripts and scheduled jobs, and they never open the menu because they are not started from a terminal without arguments.

Arrow keys and type-to-filter would need JLine 3 and its native terminal handling. We add it only if the numbered menu proves too slow in daily use.

The wizards read stdin with their own reader. That works on a terminal, where input arrives a line at a time. Piping a script into the menu that also drives a wizard is not supported.
