# Lead Hunter web

The browser client for Lead Hunter (ADR 0030). TanStack Start with Router, Query and Table, Tailwind and shadcn/ui primitives, Zod, Biome, Vitest and Playwright. With `LEADHUNTER_API_URL` set on the web server every page reads the HTTP API through server functions (ADR 0031, 0037); without it the pages run on sample data (ADR 0035).

## Run it

Needs Node 22 or newer and pnpm (`corepack enable`).

```bash
cd web
pnpm install
pnpm dev                    # http://localhost:3000
```

Point the pages at the backend instead of the fixtures (needs the `web`
Spring profile running, see `../docs/api.md`):

```bash
LEADHUNTER_API_URL=http://localhost:8080/api pnpm dev
```

## Commands

| Command | What |
|---|---|
| `pnpm dev` | Dev server with hot reload |
| `pnpm build` then `pnpm start` | Production build and server (`PORT`, default 3000) |
| `pnpm lint` / `pnpm format` | Biome check / Biome fix |
| `pnpm typecheck` | Generate the route tree, then `tsc` |
| `pnpm test` | Vitest unit tests |
| `pnpm test:e2e` | Smoke tests (fixtures build) plus the API integration tests (mock-API build, `playwright.integration.config.ts`). Needs `pnpm exec playwright install chromium` once |
| `pnpm screenshots` | Writes one image per page to `../docs/screenshots/web` |

`./check web` from the repo root runs everything the definition of done needs.

## Layout

- `src/routes/` file-based routes, one per page. Loaders prefetch the page's queries for server rendering.
- `src/features/<feature>/` one folder per feature (`leads`, `campaigns`, `runs`, `usage`, `company`):
  - `schema.ts` Zod schemas for the API responses, mirroring the backend enums.
  - `api.ts` the TanStack Start server functions (`createServerFn`) the queries call. The browser only ever calls these; only the web server talks to the backend (ADR 0037).
  - `api.server.ts` the only file that knows where data comes from: the HTTP API through `lib/http.server.ts` when `LEADHUNTER_API_URL` is set (with `LEADHUNTER_API_TOKEN` as a bearer token when set), `fixtures.ts` through `lib/fake-api.ts` otherwise. It maps the backend shapes in `lib/api-contract.ts` to the feature schema. Plain functions, so the unit tests call them directly.
  - `queries.ts` TanStack Query options. Pages read data only through these.
  - `model.ts` labels, tones and formatting helpers. Never scoring: scores and reasons come from the API (ADR 0007).
  - `components/` the feature's UI.
- `src/components/ui/` the design system: button, card, chip, cost, kbd, progress, field, segmented, option button, skeleton, data table, dialog, tooltip, command palette.
- `src/components/` the app shell (sidebar, top bar, mobile tab bar, ⌘K palette) and shared pieces.
- `src/lib/` formatting (`format.ts`), theme, hotkeys, the HTTP client and backend contract, the fake API, and `test-api.ts` for unit tests that stub the backend.
- `src/styles.css` design tokens. Use the token classes (`bg-panel`, `text-mute`, `border-line`, `bg-acc`, `text-ok`...) rather than raw colours, so dark mode keeps working.

## Shortcuts

`⌘K` or `Ctrl+K` opens the palette. `G` then `H`, `C`, `L`, `E`, `U` or `S` goes to a section. `N` starts a campaign, `T` switches the theme. On a lead, `J` and `K` move between leads and `W` opens WhatsApp.
