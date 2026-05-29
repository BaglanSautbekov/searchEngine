package com.baglan.searchEngine.api;


import com.baglan.searchEngine.api.dto.CrawlJobResponse;
import com.baglan.searchEngine.api.dto.CreateCrawlJobRequest;
import com.baglan.searchEngine.api.dto.CreateCrawlJobResponse;
import com.baglan.searchEngine.crawl.CrawlJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {
    private final CrawlJobService crawlJobService;

    public CrawlController(CrawlJobService crawlJobService) {
        this.crawlJobService = crawlJobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateCrawlJobResponse create(@Valid @RequestBody CreateCrawlJobRequest request) {
        return crawlJobService.create(request.startUrl(), request.maxPages());
    }

    @GetMapping("/{jobId}")
    public CrawlJobResponse getById(@PathVariable UUID jobId) {
        return crawlJobService.getById(jobId);
    }
}