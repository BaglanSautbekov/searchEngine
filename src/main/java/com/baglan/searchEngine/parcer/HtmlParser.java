package com.baglan.searchEngine.parcer;


import com.baglan.searchEngine.crawler.UrlNormalizer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class HtmlParser {
    private static final int MAX_BODY_TEXT_LENGTH = 100_000;

    private final UrlNormalizer urlNormalizer;

    public HtmlParser(UrlNormalizer urlNormalizer) {
        this.urlNormalizer = urlNormalizer;
    }

    public ParsedPage parse(String url, String normalizedUrl, Document document, int statusCode) {
        Document cleanDocument = document.clone();
        cleanDocument.select("script, style, noscript").remove();

        String title = normalizeText(cleanDocument.title());
        String description = extractMetaDescription(cleanDocument);
        String h1 = extractFirstH1(cleanDocument);
        String bodyText = extractBodyText(cleanDocument);
        String canonicalUrl = extractCanonicalUrl(normalizedUrl, cleanDocument);
        String contentHash = sha256(bodyText);

        return new ParsedPage(
                url,
                normalizedUrl,
                title,
                description,
                h1,
                bodyText,
                canonicalUrl,
                contentHash,
                statusCode,
                Instant.now()
        );
    }

    private String extractMetaDescription(Document document) {
        Element description = document.selectFirst("meta[name=description], meta[property=og:description]");
        if (description == null) {
            return "";
        }

        return normalizeText(description.attr("content"));
    }

    private String extractFirstH1(Document document) {
        Element h1 = document.selectFirst("h1");
        if (h1 == null) {
            return "";
        }

        return normalizeText(h1.text());
    }

    private String extractBodyText(Document document) {
        String text = document.body() == null ? document.text() : document.body().text();
        String normalized = normalizeText(text);

        if (normalized.length() <= MAX_BODY_TEXT_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, MAX_BODY_TEXT_LENGTH);
    }

    private String extractCanonicalUrl(String baseUrl, Document document) {
        Element canonical = document.selectFirst("link[rel=canonical]");
        if (canonical == null) {
            return null;
        }

        String href = canonical.attr("href");
        if (href == null || href.isBlank()) {
            return null;
        }

        return urlNormalizer.normalizeRelative(baseUrl, href).orElse(null);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("\\s+", " ").trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("content_hash_failed", ex);
        }
    }
}