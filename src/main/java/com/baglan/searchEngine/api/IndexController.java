package com.baglan.searchEngine.api;

import com.baglan.searchEngine.api.dto.IndexOperationResponse;
import com.baglan.searchEngine.index.PageIndexService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/index")
public class IndexController {
    private final PageIndexService pageIndexService;

    public IndexController(PageIndexService pageIndexService) {
        this.pageIndexService = pageIndexService;
    }

    @DeleteMapping
    public IndexOperationResponse deleteIndex() {
        pageIndexService.deleteIndex();

        return new IndexOperationResponse(
                pageIndexService.getIndexName(),
                "DELETED",
                Instant.now()
        );
    }

    @PostMapping("/recreate")
    public IndexOperationResponse recreateIndex() {
        pageIndexService.recreateIndex();

        return new IndexOperationResponse(
                pageIndexService.getIndexName(),
                "RECREATED",
                Instant.now()
        );
    }
}