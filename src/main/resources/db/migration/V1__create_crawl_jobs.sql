create table crawl_jobs (
    id uuid primary key,
    start_url text not null,
    status varchar(32) not null,
    max_pages integer not null,
    pages_discovered integer not null default 0,
    pages_indexed integer not null default 0,
    error_message text null,
    created_at_utc timestamptz not null,
    started_at_utc timestamptz null,
    finished_at_utc timestamptz null,

    constraint chk_crawl_jobs_status check (
        status in ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')
    ),

    constraint chk_crawl_jobs_max_pages check (
        max_pages >= 1 and max_pages <= 1000
    ),

    constraint chk_crawl_jobs_pages_discovered check (
        pages_discovered >= 0
    ),

    constraint chk_crawl_jobs_pages_indexed check (
        pages_indexed >= 0
    )
);

create index idx_crawl_jobs_status on crawl_jobs(status);
create index idx_crawl_jobs_created_at_utc on crawl_jobs(created_at_utc desc);