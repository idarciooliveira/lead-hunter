# 0042. Sign in with a password or a magic link sent through Resend; accounts come from invitations and the CLI

- Date: 2026-10-06
- Status: Accepted
- Supersedes: the account-creation part of [0038](0038-logins-with-better-auth.md)

## Context

ADR 0038 picked Better Auth with email and password, no public sign-up, and accounts created by the owner. It did not say how the owner creates them. The tool will now serve several organizations (ADR 0043), so people outside the owner's company will need accounts. The owner wants the CLI to do everything the web app and the API do, account creation included.

Sign-in links, invitations and forgotten passwords all need email, and the backend sends none today. Better Auth hashes passwords with scrypt (N=16384, r=16, p=1, 64-byte key, NFKC-normalised password, 16-byte salt written as hex) and stores `salt:key` in the account row. The JDK has no scrypt.

## Decision

- Users sign in with email and password, or with a magic link (Better Auth's `magicLink` plugin). There is no password reset page; a magic link covers a forgotten password.
- The web server sends email through Resend, behind a `Mailer` interface. `RESEND_API_KEY` and `LEADHUNTER_MAIL_FROM` come from the environment, and the sending domain is verified in Resend. Dev and tests use a mailer that logs the link, so no test calls Resend. With `LEADHUNTER_API_URL` set in production, a missing key stops the web server at startup.
- There is no public sign-up yet. An account comes from an organization invitation (ADR 0043) or from the CLI. The magic-link plugin runs with sign-up disabled, so a link requested for an unknown email creates nothing. Public sign-up waits until budgets are enforced (ADR 0044) and gets its own ADR.
- The CLI adds `users add <email> --name <name> --org <slug>`, `users list` and `users remove <email>`. The password is read with echo off. Java writes Better Auth's hash format with Bouncy Castle's scrypt (`bcprov`). Two committed fixtures pin the format: Java verifies a hash Better Auth produced, and a web test verifies a hash Java produced.
- Accounts created by the CLI or through an invitation are marked verified, because the owner or the invitation email already proved the address.
- The auth tables come from Better Auth's generated schema through a Flyway migration, as ADR 0038 says. The user model is renamed `app_user`, because `user` is reserved in Postgres, and columns use snake_case like the rest of the schema; the web config maps Better Auth's field names to them.
- Without `LEADHUNTER_API_URL` the web app shows sample data (ADR 0035) and skips the login.

## Consequences

Signing in by link and inviting someone depend on Resend; when it is down, passwords still work. The backend gains Bouncy Castle, and Java holds a copy of a hash format another library owns. If a Better Auth upgrade changes it, the fixture tests fail before anyone gets locked out. Users who forget a password and cannot receive email need the owner to reset it from the CLI.
