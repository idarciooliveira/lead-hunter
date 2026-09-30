-- Stage 2 enrichment: website crawls and scraped reviews. See ADR 0027.

-- One crawl attempt of a place's website. Sealed rules read it in Stage2Scorer;
-- a new row is inserted per attempt, enrichment reads the latest one.
create table website_crawl (
    id              bigserial primary key,
    place_id        bigint      not null references place (id) on delete cascade,
    url             text        not null,
    reachable       boolean     not null,
    https           boolean,
    mobile_friendly boolean,
    stale           boolean,
    content_hash    text,
    http_status     integer,
    error           text,
    crawled_at      timestamptz not null default now()
);

create index website_crawl_place_idx on website_crawl (place_id, crawled_at desc);

-- Reviews scraped for an enriched place by the Apify Google Maps actor.
create table place_review (
    id           bigserial primary key,
    place_id     bigint      not null references place (id) on delete cascade,
    star         integer,
    text         text,
    published_at text,
    created_at   timestamptz not null default now()
);

create index place_review_place_idx on place_review (place_id);

-- Stage 2 columns on the lead: complaint classes the LLM found and when enrichment ran.
alter table lead add column complaint_kinds text[] not null default '{}';
alter table lead add column enriched_at timestamptz;
