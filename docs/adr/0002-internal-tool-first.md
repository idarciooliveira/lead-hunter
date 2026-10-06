# 0002. Build an internal tool for our own lead generation first

- Date: 2026-09-28
- Status: Accepted, partly superseded by [0043](0043-organizations-as-tenants.md)

## Context

The owner runs a software factory that builds websites, apps, and does consulting for small companies. Getting new clients is the problem. A lead generation product for other business owners is a possible future, but nobody has proven the approach works yet.

## Decision

Lead Hunter is an internal tool with one user: the owner. It has no signup, no multi-tenancy, and no billing.

We still model the input as a "campaign" built from the 10 onboarding questions, so the same shape can become the onboarding flow if the tool turns into a product.

## Consequences

We skip auth, tenancy, and payments, which cuts weeks of work. We get real feedback quickly because the only user depends on the results. If it becomes a product later, we will need to add tenancy and auth to a schema that assumes one user.
