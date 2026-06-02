package com.baglan.searchEngine.search;

import com.baglan.searchEngine.api.dto.SearchResponse;
import com.baglan.searchEngine.api.dto.SearchResultItem;
import com.baglan.searchEngine.search.SearchException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class SearchService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String openSearchUrl;
    private final String indexName;

    public SearchService(
            ObjectMapper objectMapper,
            @Value("${app.opensearch.url}") String openSearchUrl,
            @Value("${app.opensearch.index-name}") String indexName
    ) {
        this.objectMapper = objectMapper;
        this.openSearchUrl = trimTrailingSlash(openSearchUrl);
        this.indexName = indexName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public SearchResponse search(String query, int page, int pageSize, UUID jobId) {
        String normalizedQuery = normalizeQuery(query);
        int from = (page - 1) * pageSize;

        try {
            String requestBody = objectMapper.writeValueAsString(buildSearchRequest(normalizedQuery, from, pageSize, jobId));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openSearchUrl + "/" + indexName + "/_search"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new SearchException("search_index_not_found");
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SearchException("opensearch_search_failed_status_" + response.statusCode());
            }

            return parseResponse(normalizedQuery, page, pageSize, response.body());
        } catch (SearchException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SearchException("opensearch_search_failed", ex);
        }
    }

    private Map<String, Object> buildSearchRequest(String query, int from, int size, UUID jobId) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("from", from);
        root.put("size", size);
        root.put("track_total_hits", true);

        Map<String, Object> multiMatch = new LinkedHashMap<>();
        multiMatch.put("query", query);
        multiMatch.put("fields", List.of("title^5", "h1^3", "description^2", "bodyText^1"));
        multiMatch.put("type", "best_fields");
        multiMatch.put("operator", "or");

        Map<String, Object> queryBody;

        if (jobId == null) {
            queryBody = Map.of("multi_match", multiMatch);
        } else {
            Map<String, Object> jobFilter = Map.of("term", Map.of("jobId", jobId.toString()));

            Map<String, Object> bool = new LinkedHashMap<>();
            bool.put("must", List.of(Map.of("multi_match", multiMatch)));
            bool.put("filter", List.of(jobFilter));

            queryBody = Map.of("bool", bool);
        }

        root.put("query", queryBody);
        root.put("highlight", buildHighlight());
        root.put("_source", List.of("title", "url", "description", "h1", "bodyText", "indexedAtUtc"));

        return root;
    }

    private Map<String, Object> buildHighlight() {
        Map<String, Object> bodyText = new LinkedHashMap<>();
        bodyText.put("fragment_size", 180);
        bodyText.put("number_of_fragments", 2);

        Map<String, Object> description = new LinkedHashMap<>();
        description.put("fragment_size", 160);
        description.put("number_of_fragments", 1);

        Map<String, Object> h1 = new LinkedHashMap<>();
        h1.put("fragment_size", 120);
        h1.put("number_of_fragments", 1);

        Map<String, Object> title = new LinkedHashMap<>();
        title.put("fragment_size", 120);
        title.put("number_of_fragments", 1);

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("bodyText", bodyText);
        fields.put("description", description);
        fields.put("h1", h1);
        fields.put("title", title);

        Map<String, Object> highlight = new LinkedHashMap<>();
        highlight.put("pre_tags", List.of("<mark>"));
        highlight.put("post_tags", List.of("</mark>"));
        highlight.put("fields", fields);

        return highlight;
    }

    private SearchResponse parseResponse(String query, int page, int pageSize, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode hits = root.path("hits");
            long total = hits.path("total").path("value").asLong(0);

            List<SearchResultItem> items = new ArrayList<>();

            for (JsonNode hit : hits.path("hits")) {
                JsonNode source = hit.path("_source");

                String title = source.path("title").asText("");
                String url = source.path("url").asText("");
                String description = source.path("description").asText("");
                String h1 = source.path("h1").asText("");
                String bodyText = source.path("bodyText").asText("");
                String indexedAtUtcValue = source.path("indexedAtUtc").asText(null);

                String snippet = extractHighlight(hit)
                        .orElseGet(() -> buildFallbackSnippet(query, title, h1, description, bodyText));

                items.add(new SearchResultItem(
                        title,
                        url,
                        snippet,
                        hit.path("_score").asDouble(0),
                        parseInstant(indexedAtUtcValue)
                ));
            }

            return new SearchResponse(query, page, pageSize, total, items);
        } catch (Exception ex) {
            throw new SearchException("opensearch_search_response_parse_failed", ex);
        }
    }

    private Optional<String> extractHighlight(JsonNode hit) {
        JsonNode highlight = hit.path("highlight");

        List<String> preferredFields = List.of("bodyText", "description", "h1", "title");
        List<String> fragments = new ArrayList<>();

        for (String field : preferredFields) {
            JsonNode fieldFragments = highlight.path(field);
            if (!fieldFragments.isArray()) {
                continue;
            }

            for (JsonNode fragment : fieldFragments) {
                String value = fragment.asText("");
                if (!value.isBlank()) {
                    fragments.add(value);
                }
            }
        }

        if (fragments.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(String.join(" ... ", fragments));
    }

    private String buildFallbackSnippet(String query, String title, String h1, String description, String bodyText) {
        String source = firstNotBlank(bodyText, description, h1, title);

        if (source.isBlank()) {
            return "";
        }

        String lowerSource = source.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        int index = lowerSource.indexOf(lowerQuery);

        if (index < 0) {
            for (String token : lowerQuery.split("\\s+")) {
                if (token.isBlank()) {
                    continue;
                }

                index = lowerSource.indexOf(token);
                if (index >= 0) {
                    break;
                }
            }
        }

        if (index < 0) {
            return source.length() <= 240 ? source : source.substring(0, 240) + "...";
        }

        int start = Math.max(0, index - 100);
        int end = Math.min(source.length(), index + lowerQuery.length() + 140);

        String prefix = start > 0 ? "..." : "";
        String suffix = end < source.length() ? "..." : "";

        return prefix + source.substring(start, end).trim() + suffix;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeQuery(String query) {
        if (query == null || query.trim().isBlank()) {
            throw new IllegalArgumentException("search_query_required");
        }

        return query.trim().replaceAll("\\s+", " ");
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new SearchException("opensearch_url_missing");
        }

        String result = value.trim();

        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }
}