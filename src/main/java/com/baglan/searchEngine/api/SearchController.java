package com.baglan.searchEngine.api;

import com.baglan.searchEngine.api.dto.SearchResponse;
import com.baglan.searchEngine.search.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponse search(
            @RequestParam @NotBlank(message = "q is required") String q,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than or equal to 1") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than or equal to 1") @Max(value = 50, message = "pageSize must be less than or equal to 50") int pageSize,
            @RequestParam(required = false) UUID jobId
    ) {
        return searchService.search(q, page, pageSize, jobId);
    }
}