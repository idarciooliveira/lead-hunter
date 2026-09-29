# 0026. Soft-clear the screen in interactive mode

- Date: 2026-09-29
- Status: Accepted

## Context

The interactive menu and the two wizards print one screen after another. After a
menu choice, a stage switch in the lead browser, or the previous question block
of `campaign new` / `company setup`, the old output stays on screen and the new
content piles up below it, so the operator scrolls to find what changed.

Tests and piped runs capture output with a `StringWriter` or a pipe; they must
never contain terminal escape sequences. The existing rule for that class of
behavior is ADR 0024 (color only when stdout is a terminal) and its duplicate
terminal detection.

## Decision

Clear the screen with the soft sequence `ESC[H ESC[2J` (cursor home + clear
screen) before rendering a new block in interactive mode:

- the menu redraws its main list between choices,
- the campaign chooser before it opens, and the company-profile chooser,
- the lead browser before each stage switch (q/b/e/a),
- each `section(...)` block of the campaign wizard and the company wizard.

The clear is a no-op when stdout is not a terminal: piped runs and tests see no
escape sequences. Older content stays available in the scrollback; the clear
only wipes the visible screen, not the scroll buffer (`ESC[3J` is not sent).

Terminal detection on `Console.isTerminal()` (Java 22+) moves to one `Terminal`
class; `Menu.colorWanted` and `MenuCommand.interactive` become its callers.

Validation notes such as "Required." or "Not added. Use: ..." are never cleared:
they must stay next to the prompt the user has to fix.

## Consequences

Easier: each interactive screen reads on its own; the operator no longer scrolls
past stale lists and previous question blocks.

Harder: output in a terminal is no longer an append-only log. Anything the
operator wants to keep from a cleared screen must be captured before moving on.

Accepted: the soft clear leaves scrollback intact, so cleared content stays
reachable by scrolling up in most terminals.
