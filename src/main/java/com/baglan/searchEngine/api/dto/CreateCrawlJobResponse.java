package com.baglan.searchEngine.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateCrawlJobResponse(
        UUID jobId,
        String startUrl,
        String status,
        int maxPages,
        Instant createdAtUtc
) {
}