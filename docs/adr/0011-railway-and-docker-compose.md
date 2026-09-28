# 0011. Deploy on Railway; run locally with Docker Compose

- Date: 2026-09-28
- Status: Accepted

## Context

The owner wants the tool deployed so it works away from the laptop, and still runnable locally when needed.

## Decision

- A multi-stage Dockerfile builds the jar with Maven and runs it on a Java 21 JRE image.
- Railway hosts the app and a Railway Postgres database. The datasource reads Railway's `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` variables, with local defaults.
- `docker-compose.yml` starts Postgres for local work.
- Secrets such as `APIFY_TOKEN` and `ANTHROPIC_API_KEY` live only in environment variables.

## Consequences

The same image runs everywhere. The Railway Hobby plan costs about $5 a month on top of the data budget in ADR 0006.
