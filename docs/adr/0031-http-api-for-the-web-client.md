# 0031. Add a JSON HTTP API to the backend for the web client

- Date: 2026-10-04
- Status: Proposed
- Amends: [0009](0009-cli-first.md)

## Context

The backend is a non-web Spring Boot application, and everything is reached through picocli commands. ADR 0009 said the web UI would come as a Spring profile in the same application, reusing the same services. The domain logic already sits in the feature packages: `LeadRepository`, `CampaignRepository`, `RunRepository`, `CampaignRunner`, `EnrichmentRunner` and `LlmCallRepository`.

## Decision

- Add `spring-boot-starter-webmvc` and a package `me.iofdev.leadhunter.api` with controllers and DTO records. Controllers call the existing repositories and runners. They hold no business rules.
- The web server starts only under a Spring profile, `web`. The CLI keeps running as a non-web application.
- Paths are under `/api`. The first read endpoints are: leads (filter by campaign, stage, status and minimum score), lead detail, campaigns, company profile and usage. Write endpoints follow: lead status and lost reason (ADR 0012, 0020), campaign create and update, company update, and starting runs.
- Jackson 3 (`tools.jackson.*`) serializes everything. Errors return a JSON body with a message and the matching status, mirroring the CLI's `error: <message>`.
- Campaign and company writes use the same validation as the CLI (ADR 0029).
- API tests use `MockMvc` against real Postgres, per ADR 0016. No test calls Apify or an LLM.

## Consequences

Logic that both adapters need moves into the feature packages, so the CLI and API cannot drift. The application gets a second mode and a servlet dependency. Endpoints that spend money (runs, enrichment) need the guards in ADR 0033.
