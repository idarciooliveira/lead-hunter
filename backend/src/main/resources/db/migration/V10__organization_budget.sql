-- Budgets that refuse runs (ADR 0044).
-- Runs carry their organization, so deleting a campaign keeps what its runs cost: campaign_id becomes
-- nullable and is set to null on delete instead of cascading. Nothing removes spend from a month any more.
-- estimated_usd is what a job or run was expected to cost when it started. A run whose cost is missing
-- counts at it, and a running job counts at it until its runs report their own cost.
-- organization.monthly_budget_usd is the operator's cap for one organization; null means the default
-- from leadhunter.usage.monthly-budget-usd.

alter table campaign_run add column org_id text references organization (id) on delete cascade;
update campaign_run r set org_id = (select c.org_id from campaign c where c.id = r.campaign_id);
alter table campaign_run alter column org_id set not null;
create index campaign_run_org_started_idx on campaign_run (org_id, started_at);

alter table campaign_run alter column campaign_id drop not null;
alter table campaign_run drop constraint campaign_run_campaign_id_fkey;
alter table campaign_run add constraint campaign_run_campaign_id_fkey
    foreign key (campaign_id) references campaign (id) on delete set null;

alter table campaign_run add column estimated_usd numeric(10, 4);

alter table organization add column monthly_budget_usd numeric(10, 2);
