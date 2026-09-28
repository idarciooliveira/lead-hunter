-- Schema for stage 1 of the pipeline. See ADR 0006 and ADR 0010.

create table campaign (
    id         bigserial primary key,
    slug       text        not null unique,
    name       text        not null,
    answers    jsonb       not null,
    search     jsonb       not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- One Google Maps place, shared by every campaign that finds it.
create table place (
    id                 bigserial primary key,
    google_place_id    text             not null unique,
    name               text             not null,
    category           text,
    categories         text[]           not null default '{}',
    address            text,
    neighborhood       text,
    city               text,
    phone_raw          text,
    phone_e164         text,
    phone_mobile       boolean          not null default false,
    website            text,
    website_kind       text             not null,
    rating             numeric(2, 1),
    reviews_count      integer          not null default 0,
    latitude           double precision,
    longitude          double precision,
    maps_url           text,
    permanently_closed boolean          not null default false,
    temporarily_closed boolean          not null default false,
    raw                jsonb            not null,
    first_seen_at      timestamptz      not null default now(),
    last_scraped_at    timestamptz      not null default now()
);

-- One scraper run: all search terms of a campaign in one location. cost_usd tracks the budget.
create table campaign_run (
    id              bigserial primary key,
    campaign_id     bigint        not null references campaign (id) on delete cascade,
    location        text          not null,
    search_terms    text[]        not null,
    max_places      integer       not null,
    status          text          not null,
    external_run_id text,
    dataset_id      text,
    places_found    integer,
    cost_usd        numeric(10, 4),
    error           text,
    started_at      timestamptz   not null default now(),
    finished_at     timestamptz
);

create index campaign_run_campaign_idx on campaign_run (campaign_id);

-- A place as a lead in one campaign. stage comes from the pipeline, status from the user.
create table lead (
    id              bigserial primary key,
    campaign_id     bigint      not null references campaign (id) on delete cascade,
    place_id        bigint      not null references place (id),
    first_run_id    bigint      references campaign_run (id) on delete set null,
    stage           text        not null,
    score           integer     not null default 0,
    score_breakdown jsonb       not null default '[]',
    stage_reason    text,
    status          text        not null default 'NEW',
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    unique (campaign_id, place_id)
);

create index lead_campaign_stage_score_idx on lead (campaign_id, stage, score desc);
