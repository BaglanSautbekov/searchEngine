alter table crawl_jobs
    add column if not exists pages_stored integer not null default 0,
    add column if not exists duplicate_pages_skipped integer not null default 0;

alter table crawl_jobs
    add constraint chk_crawl_jobs_pages_stored
    check (pages_stored >= 0);

alter table crawl_jobs
    add constraint chk_crawl_jobs_duplicate_pages_skipped
    check (duplicate_pages_skipped >= 0);

alter table crawled_pages
    add column if not exists description text not null default '',
    add column if not exists h1 text not null default '',
    add column if not exists body_text text,
    add column if not exists canonical_url text null,
    add column if not exists content_hash varchar(64),
    add column if not exists status_code integer,
    add column if not exists fetched_at_utc timestamptz;

update crawled_pages
set
    body_text = coalesce(body_text, raw_text, ''),
    status_code = coalesce(status_code, http_status),
    fetched_at_utc = coalesce(fetched_at_utc, created_at_utc),
    content_hash = coalesce(
        content_hash,
        encode(sha256(convert_to(coalesce(raw_text, ''), 'UTF8')), 'hex')
    )
where body_text is null
   or status_code is null
   or fetched_at_utc is null
   or content_hash is null;

alter table crawled_pages
    alter column body_text set not null,
    alter column content_hash set not null,
    alter column status_code set not null,
    alter column fetched_at_utc set not null;

alter table crawled_pages
    alter column raw_text drop not null,
    alter column http_status drop not null,
    alter column created_at_utc drop not null;

alter table crawled_pages
    add constraint chk_crawled_pages_status_code
    check (status_code >= 100 and status_code <= 599);

create index if not exists idx_crawled_pages_content_hash
    on crawled_pages(content_hash);

create unique index if not exists uq_crawled_pages_job_content_hash
    on crawled_pages(crawl_job_id, content_hash);