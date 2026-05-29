package com.baglan.searchEngine.crawler;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CrawledPageRepository extends JpaRepository<CrawledPage, UUID> {
    boolean existsByCrawlJobIdAndNormalizedUrl(UUID crawlJobId, String normalizedUrl);
}