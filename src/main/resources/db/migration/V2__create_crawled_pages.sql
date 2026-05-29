create table crawled_pages (
    id uuid primary key,
    crawl_job_id uuid not null references crawl_jobs(id) on delete cascade,
    url text not null,
    normalized_url text not null,
    title text null,
    raw_text text not null,
    http_status integer not null,
    created_at_utc timestamptz not null,

    constraint uq_crawled_pages_job_normalized_url unique (crawl_job_id, normalized_url),
    constraint chk_crawled_pages_http_status check (http_status >= 100 and http_status <= 599)
);

create index idx_crawled_pages_crawl_job_id on crawled_pages(crawl_job_id);
create index idx_crawled_pages_created_at_utc on crawled_pages(created_at_utc desc);