# 0035. Build the web screens on sample data behind a fake API, with the prototype's design tokens

- Date: 2026-10-05
- Status: Proposed
- Amends: [0030](0030-web-client-with-tanstack-start.md), [0034](0034-check-script-and-ci-define-done.md)

## Context

ADR 0030 chose TanStack Start, Query, Router and Table, Tailwind with shadcn/ui, Zod, Vitest and Playwright for `web/`. The HTTP API from ADR 0031 does not exist yet. A clickable prototype (`Lead Hunter.html`, desktop plus three mobile screens, in Portuguese) settles the look: Geist and Geist Mono, a warm neutral palette with an orange accent, light and dark themes. ADR 0030 does not say how the client is linted, how it gets data before the API lands, or where the look is defined. ADR 0034 left the `web/` steps of `./check` for when the app landed.

## Decision

- Every page from the prototype is built now, on sample data. Each feature has an `api.ts` with the functions the API will serve (`fetchLeads`, `fetchCampaign`, ...). Until the API exists they return fixtures from the feature's `fixtures.ts`, parsed through the same Zod schema the real responses will use (`lib/fake-api.ts`). Pages read data only through TanStack Query options in `queries.ts`, and route loaders prefetch them for server rendering. Swapping in `fetch("/api/...")` touches only the `api.ts` files.
- Schemas mirror the backend: `LeadStage`, `LeadStatus` and the `NOT_NOW` lost reason (ADR 0012, 0020). Scores, score reasons, audits and pitches are fields in the fixtures, as the API will return them. The UI formats them but never computes a score (ADR 0007).
- Actions that would write or spend (mark a lead, run, enrich, save the profile) change local component state only and are marked "Planeado" where the backend step is still planned.
- The prototype's colours are CSS variables in `src/styles.css`, exposed to Tailwind as `bg-panel`, `text-mute`, `bg-acc` and so on, with a `.dark` override. The shadcn variable names point at the same values. Shared components in `src/components/ui` (button, card, chip, data table, dialog, tooltip, palette and the rest) are the design system. Fonts come from Fontsource, so nothing loads from a CDN.
- The UI is in Portuguese, as the prototype is. Dates show in `Africa/Luanda` time.
- Biome lints and formats `web/`, with tabs and 120 columns.
- `./check` runs, after the backend, `pnpm install --frozen-lockfile`, Biome, `tsc`, Vitest, the production build and the Playwright smoke tests. `./check backend` and `./check web` run one half. CI runs the halves as two jobs.
- `pnpm screenshots` writes one image per page to `docs/screenshots/web`.

## Consequences

Every screen can be reviewed and tested before the API exists, and the API work is mostly replacing fixture reads with `fetch`. The fixtures can drift from what the API returns; the schemas catch shape changes, not meaning. Buttons that look live do nothing that persists, which the "Planeado" chips and the sidebar note say. `./check` now needs Node 22 or newer, pnpm and the Playwright Chromium (`pnpm exec playwright install chromium`) as well as JDK 21 and Docker. The `web` service in `docker-compose.yml` that ADR 0030 mentions waits until the client talks to the API.
