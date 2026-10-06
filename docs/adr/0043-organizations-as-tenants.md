# 0043. Make the organization the tenant, with an `org_id` column on its tables

- Date: 2026-10-06
- Status: Accepted
- Supersedes: the "one user, no multi-tenancy" part of [0002](0002-internal-tool-first.md)

## Context

ADR 0002 built Lead Hunter for one user with no tenancy. The schema still assumes that: `company` holds a single row (`id = 1`, ADR 0019), campaign slugs are unique across the install, and `llm_call` can have no campaign at all. Issue #18 asks for one person to run leads for several client companies with their data kept apart, and the owner now wants other teams to use the tool with their own profile, campaigns and budget.

`place`, `place_review` and `website_crawl` hold public Google Maps data and website content. A lead's status, outcome and pitch live on `lead`, which belongs to a campaign.

We compared a schema per tenant with an `org_id` column. A schema per tenant would mean running every Flyway migration once per tenant and copying `place` into each one.

## Decision

- The tenant is an organization. Better Auth's `organization` plugin stores organizations, members and invitations, and keeps the active organization on the session. Its roles (owner, admin, member) are used as they are: owners and admins invite people, members work the leads. This is the roles ADR that ADR 0038 asked for.
- `company`, `campaign` and `llm_call` get an `org_id`. `campaign_run` and `lead` are reached through `campaign`. Each organization has one company profile, and campaign slugs are unique per organization.
- `place`, `place_review` and `website_crawl` stay shared. An organization sees a place only through its own leads.
- The migration creates a default organization and moves every existing row into it. The owner joins it as owner from the CLI.
- Every repository method that reads or writes tenant data takes an `OrgId`. Postgres row-level security is not used for now.
- An isolation test suite calls every API endpoint and CLI command as one organization against another organization's ids, and expects a not-found every time.
- The web server sends the active organization in `X-LeadHunter-Org` next to `X-LeadHunter-User` (ADR 0037). Spring checks that the pair exists in `member` before it handles the call, and answers 403 otherwise.
- The CLI acts as the operator and skips the membership check. It takes a global `--org <slug>`, falls back to `LEADHUNTER_ORG`, then to the only organization, and fails when there are several and none was chosen. It adds `orgs add|list` and `members add`.
- `lead.outcome_by` and `campaign_run.started_by` reference `app_user`. Writes from the CLI leave them null.

## Consequences

Every tenant query carries one more parameter, and a method that forgets it leaks data. The isolation tests catch that, not the database; row-level security can be added later as a second layer. A review or crawl fetched for one organization is reused by the next one that finds the same place, and its cost stays with the organization whose run paid for it. Run leases (ADR 0036) are keyed by campaign id and need no change. Web URLs keep the campaign slug, resolved inside the active organization.
