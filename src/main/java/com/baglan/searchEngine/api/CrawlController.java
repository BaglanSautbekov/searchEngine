package com.baglan.searchEngine.api;


import com.baglan.searchEngine.api.dto.CrawlJobResponse;
import com.baglan.searchEngine.api.dto.CreateCrawlJobRequest;
import com.baglan.searchEngine.api.dto.CreateCrawlJobResponse;
import com.baglan.searchEngine.crawl.CrawlJobService;
import com.baglan.searchEngine.crawler.CrawlerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {
    private final CrawlJobService crawlJobService;
    private final CrawlerService crawlerService;

    public CrawlController(CrawlJobService crawlJobService, CrawlerService crawlerService) {
        this.crawlJobService = crawlJobService;
        this.crawlerService = crawlerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrawlJobResponse create(@Valid @RequestBody CreateCrawlJobRequest request) {
        CreateCrawlJobResponse response = crawlJobService.create(request.startUrl(), request.maxPages());
        crawlerService.crawlAsync(response.jobId());
        return response;
    }

    @GetMapping("/{jobId}")
    public CrawlJobResponse getById(@PathVariable UUID jobId) {
        return crawlJobService.getById(jobId);
    }
}