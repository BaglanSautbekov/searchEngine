package com.baglan.searchEngine.crawl;

import com.baglan.searchEngine.crawl.CrawlJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CrawlJobRepository extends JpaRepository<CrawlJob, UUID> {
}