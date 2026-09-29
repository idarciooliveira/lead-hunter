# 0024. Render a shaded fox illustration in the interactive menu

- Date: 2026-09-29
- Status: Superseded by 0025
- Amends: [0023](0023-fox-banner-in-interactive-menu.md)

## Context

The mirrored 24-by-18 pixel grid from ADR 0023 reads as a blocky mascot and cannot show natural shading, fur, or facial detail. The menu needs a more expressive, colored fox while preserving its single-writer output contract and a readable no-color mode.

## Decision

- Draw the fox as an antialiased, shaded vector illustration with Java2D and no added dependency.
- Rasterize the illustration at 72 by 52 pixels. In color mode, combine each vertical pixel pair into one upper-half-block character and emit its foreground/background colors with 24-bit ANSI sequences.
- When color is disabled or stdout is not a terminal, render the same illustration as monochrome ASCII intensity values without escape sequences.
- Keep the illustration, wordmark, and subtitle flowing through `Prompter`'s writer. Continue to print the banner once before the first menu prompt and honor `NO_COLOR`.

## Consequences

The fox can use smooth contours, layered orange and cream fur, shaded ears, and detailed eyes without a runtime library. Color mode uses 24-bit ANSI and a Unicode half-block, so older terminals may show a reduced palette or a substitute glyph; the monochrome fallback remains legible in logs and pipes. Illustration changes stay in `LeadFox` and are covered by output-contract tests.
