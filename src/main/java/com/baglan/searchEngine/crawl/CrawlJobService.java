package com.baglan.searchEngine.crawl;

import com.baglan.searchEngine.api.dto.CrawlJobResponse;
import com.baglan.searchEngine.api.dto.CreateCrawlJobResponse;
import com.baglan.searchEngine.common.NotFoundException;
import com.baglan.searchEngine.crawl.CrawlJob;
import com.baglan.searchEngine.crawl.CrawlJobRepository;
import com.baglan.searchEngine.crawl.CrawlJobStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class CrawlJobService {
    private static final int DEFAULT_MAX_PAGES = 1000;

    private final CrawlJobRepository crawlJobRepository;

    public CrawlJobService(CrawlJobRepository crawlJobRepository) {
        this.crawlJobRepository = crawlJobRepository;
    }

    @Transactional
    public CreateCrawlJobResponse create(String startUrl, Integer maxPages) {
        String normalizedStartUrl = normalizeAndValidateStartUrl(startUrl);
        int resolvedMaxPages = maxPages == null ? DEFAULT_MAX_PAGES : maxPages;

        CrawlJob crawlJob = new CrawlJob(
                UUID.randomUUID(),
                normalizedStartUrl,
                CrawlJobStatus.PENDING,
                resolvedMaxPages,
                Instant.now()
        );

        CrawlJob saved = crawlJobRepository.save(crawlJob);

        return new CreateCrawlJobResponse(
                saved.getId(),
                saved.getStartUrl(),
                saved.getStatus().name(),
                saved.getMaxPages(),
                saved.getCreatedAtUtc()
        );
    }

    @Transactional(readOnly = true)
    public CrawlJobResponse getById(UUID jobId) {
        CrawlJob job = crawlJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("crawl_job_not_found"));

        return new CrawlJobResponse(
                job.getId(),
                job.getStartUrl(),
                job.getStatus().name(),
                job.getMaxPages(),
                job.getPagesDiscovered(),
                job.getPagesStored(),
                job.getDuplicatePagesSkipped(),
                job.getPagesIndexed(),
                job.getErrorMessage(),
                job.getCreatedAtUtc(),
                job.getStartedAtUtc(),
                job.getFinishedAtUtc()
        );
    }

    private String normalizeAndValidateStartUrl(String startUrl) {
        String value = startUrl == null ? "" : startUrl.trim();

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("start_url_invalid");
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null || host == null) {
            throw new IllegalArgumentException("start_url_must_be_absolute");
        }

        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
            throw new IllegalArgumentException("start_url_must_be_http_or_https");
        }

        return uri.toString();
    }
}