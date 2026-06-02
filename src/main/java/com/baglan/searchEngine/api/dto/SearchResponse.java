package com.baglan.searchEngine.api.dto;

import com.baglan.searchEngine.api.dto.SearchResultItem;

import java.util.List;

public record SearchResponse(
        String query,
        int page,
        int pageSize,
        long total,
        List<SearchResultItem> items
) {
}