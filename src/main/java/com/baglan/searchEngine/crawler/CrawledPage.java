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

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "h1", nullable = false)
    private String h1;

    @Column(name = "body_text", nullable = false, columnDefinition = "text")
    private String bodyText;

    @Column(name = "canonical_url")
    private String canonicalUrl;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "fetched_at_utc", nullable = false)
    private Instant fetchedAtUtc;

    protected CrawledPage() {
    }

    public CrawledPage(
            UUID id,
            UUID crawlJobId,
            String url,
            String normalizedUrl,
            String title,
            String description,
            String h1,
            String bodyText,
            String canonicalUrl,
            String contentHash,
            int statusCode,
            Instant fetchedAtUtc
    ) {
        this.id = id;
        this.crawlJobId = crawlJobId;
        this.url = url;
        this.normalizedUrl = normalizedUrl;
        this.title = title;
        this.description = description;
        this.h1 = h1;
        this.bodyText = bodyText;
        this.canonicalUrl = canonicalUrl;
        this.contentHash = contentHash;
        this.statusCode = statusCode;
        this.fetchedAtUtc = fetchedAtUtc;
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

    public String getDescription() {
        return description;
    }

    public String getH1() {
        return h1;
    }

    public String getBodyText() {
        return bodyText;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public String getContentHash() {
        return contentHash;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Instant getFetchedAtUtc() {
        return fetchedAtUtc;
    }
}