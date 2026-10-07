-- Users, sessions and organizations for Better Auth (ADR 0038, 0042, 0043).
-- Shapes follow Better Auth 1.7 with the magicLink and organization plugins. The web server maps its field
-- names to these snake_case columns (lib/auth.server.ts); Better Auth's own migrator is never run.
-- Ids are text because Better Auth makes its own 32-character ids. The CLI makes ids of the same kind.

create table app_user (
    id             text        primary key,
    name           text        not null,
    email          text        not null unique,
    email_verified boolean     not null default false,
    image          text,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

create table organization (
    id         text        primary key,
    name       text        not null,
    slug       text        not null unique,
    logo       text,
    metadata   text,
    created_at timestamptz not null default now()
);

create table auth_session (
    id                     text        primary key,
    user_id                text        not null references app_user (id) on delete cascade,
    token                  text        not null unique,
    expires_at             timestamptz not null,
    ip_address             text,
    user_agent             text,
    active_organization_id text        references organization (id) on delete set null,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);
create index auth_session_user on auth_session (user_id);

-- One row per way to sign in. A password login has provider_id 'credential' and the hash in password.
create table auth_account (
    id                       text        primary key,
    user_id                  text        not null references app_user (id) on delete cascade,
    account_id               text        not null,
    provider_id              text        not null,
    password                 text,
    access_token             text,
    refresh_token            text,
    id_token                 text,
    access_token_expires_at  timestamptz,
    refresh_token_expires_at timestamptz,
    scope                    text,
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now(),
    unique (provider_id, account_id)
);
create index auth_account_user on auth_account (user_id);

-- Magic links and email verification tokens.
create table auth_verification (
    id         text        primary key,
    identifier text        not null,
    value      text        not null,
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index auth_verification_identifier on auth_verification (identifier);

create table member (
    id              text        primary key,
    organization_id text        not null references organization (id) on delete cascade,
    user_id         text        not null references app_user (id) on delete cascade,
    role            text        not null default 'member',
    created_at      timestamptz not null default now(),
    unique (organization_id, user_id)
);
create index member_user on member (user_id);

create table invitation (
    id              text        primary key,
    organization_id text        not null references organization (id) on delete cascade,
    email           text        not null,
    role            text,
    status          text        not null default 'pending',
    expires_at      timestamptz not null,
    inviter_id      text        not null references app_user (id) on delete cascade,
    created_at      timestamptz not null default now()
);
create index invitation_organization on invitation (organization_id, email);
