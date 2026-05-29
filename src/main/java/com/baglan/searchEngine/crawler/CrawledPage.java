package com.baglan.searchEngine.crawler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crawled_pages")
public class CrawledPage {
    @Id
    private UUID id;

    @Column(name = "crawl_job_id", nullable = false)
    private UUID crawlJobId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "normalized_url", nullable = false)
    private String normalizedUrl;

    @Column(name = "title")
    private String title;

    @Column(name = "raw_text", nullable = false, columnDefinition = "text")
    private String rawText;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    protected CrawledPage() {
    }

    public CrawledPage(
            UUID id,
            UUID crawlJobId,
            String url,
            String normalizedUrl,
            String title,
            String rawText,
            int httpStatus,
            Instant createdAtUtc
    ) {
        this.id = id;
        this.crawlJobId = crawlJobId;
        this.url = url;
        this.normalizedUrl = normalizedUrl;
        this.title = title;
        this.rawText = rawText;
        this.httpStatus = httpStatus;
        this.createdAtUtc = createdAtUtc;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCrawlJobId() {
        return crawlJobId;
    }

    public String getUrl() {
        return url;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getRawText() {
        return rawText;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public Instant getCreatedAtUtc() {
        return createdAtUtc;
    }
}