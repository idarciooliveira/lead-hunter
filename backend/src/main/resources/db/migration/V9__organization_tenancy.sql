-- Organizations own the company profile, the campaigns and the LLM calls (ADR 0043). Leads and runs belong to
-- an organization through their campaign. place, place_review and website_crawl stay shared.
-- Rows that exist today move into the oldest organization, or into a new one called default when there is none.

insert into organization (id, name, slug)
select 'default', 'Default', 'default'
where not exists (select 1 from organization);

alter table campaign add column org_id text references organization (id) on delete cascade;
update campaign set org_id = (select id from organization order by created_at, id limit 1);
alter table campaign alter column org_id set not null;
alter table campaign drop constraint campaign_slug_key;
alter table campaign add constraint campaign_org_slug_key unique (org_id, slug);

-- One profile per organization instead of one row for the whole install (ADR 0019).
alter table company drop constraint company_pkey;
alter table company drop column id;
alter table company add column org_id text references organization (id) on delete cascade;
update company set org_id = (select id from organization order by created_at, id limit 1);
alter table company alter column org_id set not null;
alter table company add primary key (org_id);

alter table llm_call add column org_id text references organization (id) on delete cascade;
update llm_call l set org_id = coalesce(
    (select c.org_id from campaign c where c.id = l.campaign_id),
    (select id from organization order by created_at, id limit 1));
alter table llm_call alter column org_id set not null;
create index llm_call_org_created_idx on llm_call (org_id, created_at);
