package com.baglan.searchEngine.api.dto;

import java.time.Instant;

public record IndexOperationResponse(
        String indexName,
        String status,
        Instant executedAtUtc
) {
}