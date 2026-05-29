package com.baglan.searchEngine.crawler;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UrlNormalizer {
    private static final Set<String> IGNORED_SCHEMES = Set.of("mailto", "tel", "javascript");

    public Optional<String> normalizeAbsolute(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }

        try {
            URI uri = URI.create(url.trim());
            return normalize(uri);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<String> normalizeRelative(String baseUrl, String href) {
        if (href == null || href.isBlank()) {
            return Optional.empty();
        }

        String value = href.trim();
        String lower = value.toLowerCase(Locale.ROOT);

        for (String ignoredScheme : IGNORED_SCHEMES) {
            if (lower.startsWith(ignoredScheme + ":")) {
                return Optional.empty();
            }
        }

        if (value.startsWith("#")) {
            return Optional.empty();
        }

        try {
            URI base = URI.create(baseUrl);
            URI resolved = base.resolve(value);
            return normalize(resolved);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<String> normalize(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null || host == null) {
            return Optional.empty();
        }

        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
            return Optional.empty();
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        int port = normalizePort(normalizedScheme, uri.getPort());
        String path = normalizePath(uri.getRawPath());
        String query = normalizeQuery(uri.getRawQuery());

        try {
            URI normalized = new URI(
                    normalizedScheme,
                    null,
                    normalizedHost,
                    port,
                    path,
                    query,
                    null
            );

            return Optional.of(normalized.toString());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private int normalizePort(String scheme, int port) {
        if (port == -1) {
            return -1;
        }

        if (scheme.equals("http") && port == 80) {
            return -1;
        }

        if (scheme.equals("https") && port == 443) {
            return -1;
        }

        return port;
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }

        String path = rawPath;

        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    private String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }

        return Arrays.stream(rawQuery.split("&"))
                .filter(part -> !part.isBlank())
                .map(part -> part.split("=", 2))
                .filter(pair -> !isTrackingParam(pair[0]))
                .sorted((a, b) -> a[0].compareToIgnoreCase(b[0]))
                .map(pair -> {
                    String key = encode(pair[0]);
                    String value = pair.length > 1 ? encode(pair[1]) : "";
                    return value.isBlank() ? key : key + "=" + value;
                })
                .collect(Collectors.joining("&"));
    }

    private boolean isTrackingParam(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);

        return normalized.startsWith("utm_")
                || normalized.equals("fbclid")
                || normalized.equals("gclid")
                || normalized.equals("yclid");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}