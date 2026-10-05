-- Job parents for runs started from the UI. See ADR 0033.
-- A POST creates one parent row (kind SCRAPE, ENRICH or DRY_RUN); the
-- per-location scraper rows and the reviews row become its children via
-- parent_id. Usage reporting keeps reading the children and ignores parents.
-- At most one run per campaign at a time: the partial unique index below is
-- the backstop behind the pre-check in RunJobService.

-- Stale RUNNING rows are jobs a restart interrupted; the web profile marks
-- them failed on boot from now on, so fail them here to keep the index clean.
update campaign_run set status = 'FAILED', error = 'interrupted by restart', finished_at = now()
where status = 'RUNNING';

alter table campaign_run add column kind text not null default 'SCRAPE';
-- Review batches reuse the 'reviews' location (RunRepository.REVIEWS_LOCATION).
update campaign_run set kind = 'REVIEWS' where location = 'reviews';

-- Parents span locations, so they hold no location of their own, and dry runs
-- plan but never search.
alter table campaign_run alter column location drop not null;
alter table campaign_run alter column search_terms drop not null;
alter table campaign_run alter column max_places drop not null;
alter table campaign_run add column parent_id bigint references campaign_run (id) on delete cascade;
-- Known total for enrichment (leads to enrich); null for scrapes, like the UI reads it.
alter table campaign_run add column total integer;

create unique index campaign_run_one_running_per_campaign on campaign_run (campaign_id)
where status = 'RUNNING' and parent_id is null;
