# Lead Hunter web

The browser client for Lead Hunter (ADR 0030). TanStack Start with Router, Query and Table, Tailwind and shadcn/ui primitives, Zod, Biome, Vitest and Playwright. Until the HTTP API exists (ADR 0031) every page runs on sample data; see ADR 0035.

## Run it

Needs Node 22 or newer and pnpm (`corepack enable`).

```bash
cd web
pnpm install
pnpm dev                    # http://localhost:3000
```

## Commands

| Command | What |
|---|---|
| `pnpm dev` | Dev server with hot reload |
| `pnpm build` then `pnpm start` | Production build and server (`PORT`, default 3000) |
| `pnpm lint` / `pnpm format` | Biome check / Biome fix |
| `pnpm typecheck` | Generate the route tree, then `tsc` |
| `pnpm test` | Vitest unit tests |
| `pnpm test:e2e` | Playwright smoke tests against a production build. Needs `pnpm exec playwright install chromium` once |
| `pnpm screenshots` | Writes one image per page to `../docs/screenshots/web` |

`./check web` from the repo root runs everything the definition of done needs.

## Layout

- `src/routes/` file-based routes, one per page. Loaders prefetch the page's queries for server rendering.
- `src/features/<feature>/` one folder per feature (`leads`, `campaigns`, `runs`, `usage`, `company`):
  - `schema.ts` Zod schemas for the API responses, mirroring the backend enums.
  - `api.ts` the functions the API will serve. Today they read `fixtures.ts` through `lib/fake-api.ts`; later they call `fetch`.
  - `queries.ts` TanStack Query options. Pages read data only through these.
  - `model.ts` labels, tones and formatting helpers. Never scoring: scores and reasons come from the API (ADR 0007).
  - `components/` the feature's UI.
- `src/components/ui/` the design system: button, card, chip, cost, kbd, progress, field, segmented, option button, skeleton, data table, dialog, tooltip, command palette.
- `src/components/` the app shell (sidebar, top bar, mobile tab bar, ⌘K palette) and shared pieces.
- `src/lib/` formatting (`format.ts`), theme, hotkeys, the fake API.
- `src/styles.css` design tokens. Use the token classes (`bg-panel`, `text-mute`, `border-line`, `bg-acc`, `text-ok`...) rather than raw colours, so dark mode keeps working.

## Shortcuts

`⌘K` or `Ctrl+K` opens the palette. `G` then `H`, `C`, `L`, `E`, `U` or `S` goes to a section. `N` starts a campaign, `T` switches the theme. On a lead, `J` and `K` move between leads and `W` opens WhatsApp.
