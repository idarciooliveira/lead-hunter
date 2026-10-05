-- One completed LLM call with the cost the provider reported. See ADR 0021.
-- cost_usd is null when the provider sent no cost. raw_usage keeps the provider's usage block as it came.
create table llm_call (
    id                bigserial primary key,
    campaign_id       bigint         references campaign (id) on delete set null,
    purpose           text           not null,
    model             text           not null,
    prompt_tokens     integer        not null,
    completion_tokens integer        not null,
    cost_usd          numeric(14, 8),
    generation_id     text,
    raw_usage         jsonb,
    created_at        timestamptz    not null default now()
);

create index llm_call_created_idx on llm_call (created_at);
create index llm_call_campaign_idx on llm_call (campaign_id);
