package com.baglan.searchEngine.crawl;

import com.baglan.searchEngine.crawl.CrawlJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crawl_jobs")
public class CrawlJob {
    @Id
    private UUID id;

    @Column(name = "start_url", nullable = false)
    private String startUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CrawlJobStatus status;

    @Column(name = "max_pages", nullable = false)
    private int maxPages;

    @Column(name = "pages_discovered", nullable = false)
    private int pagesDiscovered;

    @Column(name = "pages_indexed", nullable = false)
    private int pagesIndexed;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "started_at_utc")
    private Instant startedAtUtc;

    @Column(name = "finished_at_utc")
    private Instant finishedAtUtc;

    protected CrawlJob() {
    }

    public CrawlJob(UUID id, String startUrl, CrawlJobStatus status, int maxPages, Instant createdAtUtc) {
        this.id = id;
        this.startUrl = startUrl;
        this.status = status;
        this.maxPages = maxPages;
        this.pagesDiscovered = 0;
        this.pagesIndexed = 0;
        this.createdAtUtc = createdAtUtc;
    }

    public UUID getId() {
        return id;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public CrawlJobStatus getStatus() {
        return status;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public int getPagesDiscovered() {
        return pagesDiscovered;
    }

    public int getPagesIndexed() {
        return pagesIndexed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAtUtc() {
        return createdAtUtc;
    }

    public Instant getStartedAtUtc() {
        return startedAtUtc;
    }

    public Instant getFinishedAtUtc() {
        return finishedAtUtc;
    }
}