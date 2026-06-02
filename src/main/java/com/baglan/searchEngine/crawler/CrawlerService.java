package com.baglan.searchEngine.crawler;

import com.baglan.searchEngine.crawl.CrawlJob;
import com.baglan.searchEngine.crawl.CrawlJobRepository;
import com.baglan.searchEngine.crawler.*;
import com.baglan.searchEngine.parcer.HtmlParser;
import com.baglan.searchEngine.parcer.ParsedPage;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

@Service
public class CrawlerService {
    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    private final CrawlJobRepository crawlJobRepository;
    private final CrawledPageRepository crawledPageRepository;
    private final HtmlFetchClient htmlFetchClient;
    private final UrlNormalizer urlNormalizer;
    private final HtmlParser htmlParser;

    public CrawlerService(
            CrawlJobRepository crawlJobRepository,
            CrawledPageRepository crawledPageRepository,
            HtmlFetchClient htmlFetchClient,
            UrlNormalizer urlNormalizer,
            HtmlParser htmlParser
    ) {
        this.crawlJobRepository = crawlJobRepository;
        this.crawledPageRepository = crawledPageRepository;
        this.htmlFetchClient = htmlFetchClient;
        this.urlNormalizer = urlNormalizer;
        this.htmlParser = htmlParser;
    }

    @Async("crawlerTaskExecutor")
    public void crawlAsync(UUID jobId) {
        try {
            crawl(jobId);
        } catch (Exception ex) {
            log.error("Critical crawler error for jobId={}", jobId, ex);
            markJobFailed(jobId, ex.getMessage());
        }
    }

    public void crawl(UUID jobId) {
        CrawlJob job = crawlJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("crawl_job_not_found"));

        String rootHost = extractHost(job.getStartUrl());
        String normalizedStartUrl = urlNormalizer.normalizeAbsolute(job.getStartUrl())
                .orElseThrow(() -> new IllegalArgumentException("start_url_invalid"));

        log.info("Starting crawl jobId={} startUrl={} rootHost={} maxPages={}",
                job.getId(), normalizedStartUrl, rootHost, job.getMaxPages());

        markJobRunning(job);

        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Set<String> queued = new HashSet<>();

        queue.add(normalizedStartUrl);
        queued.add(normalizedStartUrl);

        int pagesDiscovered = 1;
        int pagesStored = 0;
        int duplicatePagesSkipped = 0;

        updateCounters(jobId, pagesDiscovered, pagesStored, duplicatePagesSkipped);

        while (!queue.isEmpty() && pagesStored < job.getMaxPages()) {
            String currentUrl = queue.poll();
            queued.remove(currentUrl);

            if (!visited.add(currentUrl)) {
                continue;
            }

            if (!isSameHost(currentUrl, rootHost)) {
                continue;
            }

            try {
                FetchedPage fetchedPage = htmlFetchClient.fetch(currentUrl);
                String finalNormalizedUrl = urlNormalizer.normalizeAbsolute(fetchedPage.finalUrl()).orElse(currentUrl);

                if (!isSameHost(finalNormalizedUrl, rootHost)) {
                    continue;
                }

                if (fetchedPage.httpStatus() >= 400) {
                    log.warn("Page load failed jobId={} url={} status={}",
                            jobId, currentUrl, fetchedPage.httpStatus());
                    continue;
                }

                if (!isHtml(fetchedPage.contentType())) {
                    log.warn("Non-html page skipped jobId={} url={} contentType={}",
                            jobId, currentUrl, fetchedPage.contentType());
                    continue;
                }

                Document document = fetchedPage.document();
                ParsedPage parsedPage = htmlParser.parse(
                        finalNormalizedUrl,
                        finalNormalizedUrl,
                        document,
                        fetchedPage.httpStatus()
                );

                if (crawledPageRepository.existsByCrawlJobIdAndNormalizedUrl(jobId, parsedPage.normalizedUrl())) {
                    duplicatePagesSkipped++;
                    updateCounters(jobId, pagesDiscovered, pagesStored, duplicatePagesSkipped);
                    continue;
                }

                if (crawledPageRepository.existsByCrawlJobIdAndContentHash(jobId, parsedPage.contentHash())) {
                    duplicatePagesSkipped++;
                    updateCounters(jobId, pagesDiscovered, pagesStored, duplicatePagesSkipped);
                    log.info("Duplicate content skipped jobId={} url={} contentHash={}",
                            jobId, parsedPage.normalizedUrl(), parsedPage.contentHash());
                    continue;
                }

                savePage(jobId, parsedPage);
                pagesStored++;
                updateCounters(jobId, pagesDiscovered, pagesStored, duplicatePagesSkipped);

                log.info("Page loaded jobId={} url={} status={} storedPages={}",
                        jobId, parsedPage.normalizedUrl(), parsedPage.statusCode(), pagesStored);

                for (Element link : document.select("a[href]")) {
                    if (pagesStored + queue.size() >= job.getMaxPages()) {
                        break;
                    }

                    String href = link.attr("href");
                    Optional<String> normalizedLink = urlNormalizer.normalizeRelative(finalNormalizedUrl, href);

                    if (normalizedLink.isEmpty()) {
                        continue;
                    }

                    String nextUrl = normalizedLink.get();

                    if (!isSameHost(nextUrl, rootHost)) {
                        continue;
                    }

                    if (visited.contains(nextUrl) || queued.contains(nextUrl)) {
                        continue;
                    }

                    queue.add(nextUrl);
                    queued.add(nextUrl);
                    pagesDiscovered++;
                }

                updateCounters(jobId, pagesDiscovered, pagesStored, duplicatePagesSkipped);
            } catch (Exception ex) {
                log.warn("Page load error jobId={} url={} error={}",
                        jobId, currentUrl, ex.getMessage());
            }
        }

        markJobCompleted(jobId, pagesDiscovered, pagesStored, duplicatePagesSkipped);

        log.info("Crawl completed jobId={} discoveredPages={} storedPages={} duplicatePagesSkipped={}",
                jobId, pagesDiscovered, pagesStored, duplicatePagesSkipped);
    }

    private void savePage(UUID jobId, ParsedPage parsedPage) {
        CrawledPage page = new CrawledPage(
                UUID.randomUUID(),
                jobId,
                parsedPage.url(),
                parsedPage.normalizedUrl(),
                parsedPage.title(),
                parsedPage.description(),
                parsedPage.h1(),
                parsedPage.bodyText(),
                parsedPage.canonicalUrl(),
                parsedPage.contentHash(),
                parsedPage.statusCode(),
                parsedPage.fetchedAtUtc()
        );

        try {
            crawledPageRepository.save(page);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Duplicate page ignored jobId={} url={} contentHash={}",
                    jobId, parsedPage.normalizedUrl(), parsedPage.contentHash());
        }
    }

    private boolean isHtml(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return true;
        }

        return contentType.toLowerCase().contains("text/html");
    }

    private String extractHost(String url) {
        return URI.create(url).getHost().toLowerCase();
    }

    private boolean isSameHost(String url, String rootHost) {
        try {
            String host = URI.create(url).getHost();
            return host != null && host.equalsIgnoreCase(rootHost);
        } catch (Exception ex) {
            return false;
        }
    }

    private void markJobRunning(CrawlJob job) {
        job.markRunning(Instant.now());
        crawlJobRepository.save(job);
    }

    private void updateCounters(UUID jobId, int pagesDiscovered, int pagesStored, int duplicatePagesSkipped) {
        crawlJobRepository.findById(jobId).ifPresent(job -> {
            job.updateCounters(pagesDiscovered, pagesStored, duplicatePagesSkipped);
            crawlJobRepository.save(job);
        });
    }

    private void markJobCompleted(UUID jobId, int pagesDiscovered, int pagesStored, int duplicatePagesSkipped) {
        crawlJobRepository.findById(jobId).ifPresent(job -> {
            job.markCompleted(Instant.now(), pagesDiscovered, pagesStored, duplicatePagesSkipped);
            crawlJobRepository.save(job);
        });
    }

    private void markJobFailed(UUID jobId, String errorMessage) {
        crawlJobRepository.findById(jobId).ifPresent(job -> {
            job.markFailed(Instant.now(), errorMessage == null ? "crawler_failed" : errorMessage);
            crawlJobRepository.save(job);
        });
    }
}