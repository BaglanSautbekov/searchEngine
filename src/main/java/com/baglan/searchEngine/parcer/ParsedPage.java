package com.baglan.searchEngine.parcer;


import java.time.Instant;

public record ParsedPage(
        String url,
        String normalizedUrl,
        String title,
        String description,
        String h1,
        String bodyText,
        String canonicalUrl,
        String contentHash,
        int statusCode,
        Instant fetchedAtUtc
) {
}