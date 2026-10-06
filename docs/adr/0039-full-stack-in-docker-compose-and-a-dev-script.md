# 0039. Run the API and the web client with one command, in Docker Compose or with `./dev`

- Date: 2026-10-06
- Status: Accepted

## Context

ADR 0011 gave Docker Compose only Postgres, plus the CLI under a profile. Since ADR 0030 and 0031 the tool also has an HTTP API (the backend with the `web` Spring profile) and a TanStack Start web server that calls it (ADR 0037). Running both meant two terminals: one for `SPRING_PROFILES_ACTIVE=web java -jar ...`, one for `LEADHUNTER_API_URL=... pnpm dev`, after starting Postgres by hand.

## Decision

- `web/Dockerfile` builds the web client with pnpm and runs the Nitro output on a Node 24 slim image. Nitro bundles the server's dependencies, so the runtime image holds only `.output`.
- `docker-compose.yml` gets two services that `docker compose up` starts with Postgres: `api`, the backend image with `SPRING_PROFILES_ACTIVE=web` on port 8080, and `web` on port 3000 with `LEADHUNTER_API_URL=http://api:8080/api`. Both read `.env`. `web` waits for the API's `/api/health`, the API waits for Postgres. The CLI keeps its `app` service under the `cli` profile.
- `./dev` in the repo root is the local equivalent without containers for the API and web: it starts Postgres with Compose, rebuilds the jar, runs the API with the `web` profile in the background and `pnpm dev` in the foreground. Ctrl+C stops both.

## Consequences

One command gives a working stack, with Docker as the only requirement for `docker compose up`. The web container is built for production, so it does not hot-reload; code changes need `docker compose up --build`. `./dev` is for working on the code and still needs JDK 21, Node 22+ and pnpm. Both publish 5432, 8080 and 3000 on the host, so only one of them runs at a time.
