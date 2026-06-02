package com.baglan.searchEngine.api.dto;

import java.time.Instant;

public record SearchResultItem(
        String title,
        String url,
        String snippet,
        double score,
        Instant indexedAtUtc
) {
}