package com.baglan.searchEngine.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateCrawlJobRequest(
        @NotBlank(message = "startUrl is required")
        String startUrl,

        @Min(value = 1, message = "maxPages must be greater than or equal to 1")
        @Max(value = 1000, message = "maxPages must be less than or equal to 1000")
        Integer maxPages
) {
}