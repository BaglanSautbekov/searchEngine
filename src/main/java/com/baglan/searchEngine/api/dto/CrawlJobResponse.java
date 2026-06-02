package com.baglan.searchEngine.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CrawlJobResponse(
        UUID jobId,
        String startUrl,
        String status,
        int maxPages,
        int pagesDiscovered,
        int pagesStored,
        int duplicatePagesSkipped,
        int pagesIndexed,
        String errorMessage,
        Instant createdAtUtc,
        Instant startedAtUtc,
        Instant finishedAtUtc
) {
}