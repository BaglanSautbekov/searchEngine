package com.baglan.searchEngine.index;

import com.baglan.searchEngine.crawler.CrawledPage;
import com.baglan.searchEngine.index.PageIndexingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PageIndexService {
    private static final Logger log = LoggerFactory.getLogger(PageIndexService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String openSearchUrl;
    private final String indexName;

    public PageIndexService(
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

    public String getIndexName() {
        return indexName;
    }

    public void indexPage(CrawledPage page) {
        ensureIndexExists();

        try {
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("id", page.getId().toString());
            document.put("jobId", page.getCrawlJobId().toString());
            document.put("url", page.getUrl());
            document.put("normalizedUrl", page.getNormalizedUrl());
            document.put("title", page.getTitle());
            document.put("description", page.getDescription());
            document.put("h1", page.getH1());
            document.put("bodyText", page.getBodyText());
            document.put("indexedAtUtc", Instant.now().toString());

            String body = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openSearchUrl + "/" + indexName + "/_doc/" + page.getId()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PageIndexingException("opensearch_index_page_failed_status_" + response.statusCode());
            }

            log.info("Page indexed pageId={} jobId={} url={}",
                    page.getId(), page.getCrawlJobId(), page.getNormalizedUrl());
        } catch (PageIndexingException ex) {
            log.error("Page indexing failed pageId={} jobId={} url={} error={}",
                    page.getId(), page.getCrawlJobId(), page.getNormalizedUrl(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Page indexing failed pageId={} jobId={} url={}",
                    page.getId(), page.getCrawlJobId(), page.getNormalizedUrl(), ex);
            throw new PageIndexingException("opensearch_index_page_failed", ex);
        }
    }

    public void recreateIndex() {
        deleteIndex();
        createIndex();

        log.info("OpenSearch index recreated indexName={}", indexName);
    }

    public void deleteIndex() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openSearchUrl + "/" + indexName))
                    .timeout(Duration.ofSeconds(10))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                log.info("OpenSearch index already absent indexName={}", indexName);
                return;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PageIndexingException("opensearch_delete_index_failed_status_" + response.statusCode());
            }

            log.info("OpenSearch index deleted indexName={}", indexName);
        } catch (PageIndexingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PageIndexingException("opensearch_delete_index_failed", ex);
        }
    }

    public void ensureIndexExists() {
        if (!indexExists()) {
            createIndex();
        }
    }

    private boolean indexExists() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openSearchUrl + "/" + indexName))
                    .timeout(Duration.ofSeconds(5))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 200) {
                return true;
            }

            if (response.statusCode() == 404) {
                return false;
            }

            throw new PageIndexingException("opensearch_head_index_failed_status_" + response.statusCode());
        } catch (PageIndexingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PageIndexingException("opensearch_head_index_failed", ex);
        }
    }

    private void createIndex() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openSearchUrl + "/" + indexName))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(indexMappingJson()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 400 && response.body() != null && response.body().contains("resource_already_exists_exception")) {
                return;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PageIndexingException("opensearch_create_index_failed_status_" + response.statusCode());
            }

            log.info("OpenSearch index created indexName={}", indexName);
        } catch (PageIndexingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PageIndexingException("opensearch_create_index_failed", ex);
        }
    }

    private String indexMappingJson() {
        return """
                {
                  "settings": {
                    "analysis": {
                      "filter": {
                        "russian_stop": {
                          "type": "stop",
                          "stopwords": "_russian_"
                        },
                        "english_stop": {
                          "type": "stop",
                          "stopwords": "_english_"
                        }
                      },
                      "analyzer": {
                        "ru_en_text": {
                          "type": "custom",
                          "tokenizer": "standard",
                          "filter": [
                            "lowercase",
                            "russian_stop",
                            "english_stop"
                          ]
                        }
                      }
                    }
                  },
                  "mappings": {
                    "properties": {
                      "id": {
                        "type": "keyword"
                      },
                      "jobId": {
                        "type": "keyword"
                      },
                      "url": {
                        "type": "keyword"
                      },
                      "normalizedUrl": {
                        "type": "keyword"
                      },
                      "title": {
                        "type": "text",
                        "analyzer": "ru_en_text"
                      },
                      "description": {
                        "type": "text",
                        "analyzer": "ru_en_text"
                      },
                      "h1": {
                        "type": "text",
                        "analyzer": "ru_en_text"
                      },
                      "bodyText": {
                        "type": "text",
                        "analyzer": "ru_en_text"
                      },
                      "indexedAtUtc": {
                        "type": "date"
                      }
                    }
                  }
                }
                """;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new PageIndexingException("opensearch_url_missing");
        }

        String result = value.trim();

        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }
}