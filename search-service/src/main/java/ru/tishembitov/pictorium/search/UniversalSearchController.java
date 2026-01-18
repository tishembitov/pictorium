package ru.tishembitov.pictorium.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.analytic.SearchAnalyticsService;
import ru.tishembitov.pictorium.analytic.TrendingQueryResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
public class UniversalSearchController {

    private final UniversalSearchService universalSearchService;

    @GetMapping
    public ResponseEntity<UniversalSearchResponse> searchAll(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer size,
            @RequestParam(defaultValue = "true") Boolean fuzzy
    ) {
        SearchCriteria request = SearchCriteria.builder()
                .query(q)
                .size(size)
                .fuzzy(fuzzy)
                .build();

        log.debug("Universal search request: query='{}'", q);

        return ResponseEntity.ok(universalSearchService.searchAll(request));
    }

}