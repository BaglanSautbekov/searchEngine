package com.example.minisearch.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {
    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient;
    private final String openSearchUrl;

    public HealthController(
            JdbcTemplate jdbcTemplate,
            @Value("${app.opensearch.url}") String openSearchUrl
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.openSearchUrl = openSearchUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestampUtc", Instant.now().toString());
        body.put("postgres", checkPostgres());
        body.put("opensearch", checkOpenSearch());

        boolean allUp = "UP".equals(((Map<?, ?>) body.get("postgres")).get("status"))
                && "UP".equals(((Map<?, ?>) body.get("opensearch")).get("status"));

        if (!allUp) {
            body.put("status", "DOWN");
            return ResponseEntity.status(503).body(body);
        }

        return ResponseEntity.ok(body);
    }

    private Map<String, Object> checkPostgres() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
            result.put("status", value != null && value == 1 ? "UP" : "DOWN");
        } catch (Exception ex) {
            result.put("status", "DOWN");
            result.put("error", ex.getClass().getSimpleName());
            result.put("message", ex.getMessage());
        }

        return result;
    }

    private Map<String, Object> checkOpenSearch() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openSearchUrl + "/_cluster/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            result.put("status", response.statusCode() >= 200 && response.statusCode() < 300 ? "UP" : "DOWN");
            result.put("httpStatus", response.statusCode());
        } catch (Exception ex) {
            result.put("status", "DOWN");
            result.put("error", ex.getClass().getSimpleName());
            result.put("message", ex.getMessage());
        }

        return result;
    }
}