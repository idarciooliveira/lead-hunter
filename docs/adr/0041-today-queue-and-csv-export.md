# 0041. Serve the today queue from the backend and export leads as CSV

- Date: 2026-10-06
- Status: Accepted

## Context

Step 5 of the roadmap needs a daily contact queue and a file the user can open in a spreadsheet. The web client builds the queue itself by filtering the whole lead list and cutting it at a constant of 10 (`DAILY_GOAL`). The CLI has no queue. The "Exportar Excel" button only toggles a label. The README says CSV and the web says `.xlsx`.

## Decision

- The queue is the leads with stage `QUALIFIED` and status `NEW`, across every campaign, ordered like `leads list` (score, then reviews, then id). Campaigns have no active flag, so none is filtered out. A lead leaves the queue when it is marked.
- The queue size is the company's `weeklyCapacity` divided by five, at least one (ADR 0019). A weekly capacity of 35 gives 7 a day. Before a company profile exists the default capacity applies. `--limit` and `?limit=` override it.
- `leads today`, `GET /api/leads/today`, `leads export <slug>` and `GET /api/campaigns/{slug}/leads.csv` read the same repository methods, so the clients cannot disagree.
- The export is CSV, not `.xlsx`. It needs no library, and Excel opens it. The file starts with a UTF-8 byte order mark so Excel reads the accents, uses CRLF line ends and quotes fields with commas, quotes or line breaks. A name starting with `=` or `@` gets a leading apostrophe so Excel does not run it as a formula.
- Columns: `id`, `campanha`, `nome`, `categoria`, `telefone`, `whatsapp`, `pontuacao`, `estado`, `pitch`, `motivos`. The reasons are the score breakdown joined with `; `.
- The export takes every lead of the stage, with no limit. The default stage is `QUALIFIED`.

## Consequences

Easier: the web, the CLI and any script see one queue, sized by the company's own capacity. No new dependency.

Harder: the queue size changes when the user edits the capacity, and it ignores how many contacts were already made today, so marking leads shrinks the queue instead of keeping it at a fixed daily count. A real `.xlsx` with formatting needs a library and a new ADR.
