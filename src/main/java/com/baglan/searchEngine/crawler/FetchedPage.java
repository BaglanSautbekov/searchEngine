package com.baglan.searchEngine.crawler;

import org.jsoup.nodes.Document;

public record FetchedPage(
        String finalUrl,
        int httpStatus,
        String contentType,
        Document document
) {
}