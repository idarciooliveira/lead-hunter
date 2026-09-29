# 0025. Draw the fox banner as hand-placed pixel art

- Date: 2026-09-29
- Status: Accepted
- Supersedes: [0024](0024-shaded-fox-banner.md)

## Context

The antialiased vector fox from ADR 0024 turned into a blurry, muddy 72-by-52 image once it was squeezed into terminal cells. It was also too large for the menu, and its dark gradient background clashed with the terminal's own colors.

## Decision

- Keep the fox as a hand-placed pixel sprite in `LeadFox`: the left half of the face is written as rows of palette characters and mirrored for the right half. No Java2D and no added dependency.
- Use a small fixed palette with flat shading and a dark outline, and a 42 by 38 pixel sprite so the banner takes 19 terminal rows.
- Leave the background transparent so the terminal's own background shows through.
- In color mode, combine each vertical pixel pair into a half-block character with 24-bit ANSI colors. Use a lower half block when only the bottom pixel is set and a plain space when neither is.
- When color is disabled or stdout is not a terminal, render the same sprite as monochrome ASCII intensity values without escape sequences.
- Keep the sprite, wordmark and subtitle flowing through `Prompter`'s writer, printed once before the first menu prompt, honoring `NO_COLOR`.

## Consequences

The fox stays crisp at any terminal size and looks the same on light and dark themes. Changing the art means editing the character grid and palette in `LeadFox`. Color mode still needs 24-bit ANSI and Unicode half blocks, so older terminals may show a reduced palette.
