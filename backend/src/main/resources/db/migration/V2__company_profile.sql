-- The company profile: what we sell, to whom, with what proof. One row only. See ADR 0019.
create table company (
    id         smallint    primary key default 1 check (id = 1),
    profile    jsonb       not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
