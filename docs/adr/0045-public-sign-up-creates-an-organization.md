# 0045. Open public sign-up, with one new organization per account

- Date: 2026-10-07
- Status: Proposed
- Supersedes: the "no public sign-up" lines of [0038](0038-logins-with-better-auth.md) and [0042](0042-passwords-magic-links-and-accounts-from-the-cli.md)

## Context

Accounts come only from organization invitations and the CLI (ADR 0042). The owner wants anyone to be able to sign up on the web. ADR 0042 said that waits until budgets are enforced (ADR 0044) and gets its own ADR.

Two things in the code stand in the way. A session is refused when the user belongs to no organization (`auth.server.ts`), so a bare sign-up would create an account that cannot log in. And tenancy is decided (ADR 0043) but not built: no table has `org_id`, and Spring ignores `X-LeadHunter-User` and `X-LeadHunter-Org`. A second organization would read the first one's company profile, campaigns and leads.

Every run spends the owner's Apify and AI Gateway money (ADR 0044), so an open form is also an open spending path.

## Decision

- Sign-up asks for name, email, password and organization name. Better Auth commits the user first. Then its `user.create.after` hook writes the organization and the owner membership in one database transaction, so those two succeed or fail together. The user write is not part of that transaction. Joining an existing organization still needs an invitation.
- If the hook fails, it deletes the user it just created and the sign-up returns an error, so the email can be used again. If the process dies between the user commit and the hook, the user is left with no organization. That user cannot sign in, because the session is refused without a membership and the email is not verified yet. The same provisioning runs again from `afterEmailVerification`. It does nothing when the user already has an organization, so the retry is safe.
- The email must be verified before the first session (`requireEmailVerification`). The verification link goes out through the `Mailer` from ADR 0042. This is the main abuse control.
- A new organization gets a monthly budget lower than the operator default, set by `leadhunter.usage.signup-budget-usd`. ADR 0044 enforces it and the install-wide cap.
- `/api/auth/sign-up/*` is rate limited per IP with Better Auth's built-in limiter.
- `LEADHUNTER_SIGNUP_ENABLED` turns sign-up on. It is off by default, so a deploy never opens it by accident, and the owner closes it by unsetting it.
- Sign-up does not ship before the tenancy isolation tests (ADR 0043) and budget enforcement (ADR 0044) are merged. The flag exists so the code can land earlier without being reachable.

## Consequences

Anyone with a working email address can create an organization and spend up to its budget, and the install cap bounds the total. The owner will see junk accounts, and removes them from the CLI. Verification depends on Resend: when it is down, nobody new can sign up, though existing users can still sign in with a password. There is still no billing and no bring-your-own-key, so growth is limited by what the owner is willing to pay for.
