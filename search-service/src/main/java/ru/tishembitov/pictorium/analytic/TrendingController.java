package ru.tishembitov.pictorium.analytic;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search/trending")
@RequiredArgsConstructor
@Slf4j
public class TrendingController {

    private final SearchAnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<List<TrendingQueryResponse>> getTrendingQueries(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer limit
    ) {
        log.debug("Getting trending queries, limit={}", limit);
        return ResponseEntity.ok(analyticsService.getTrendingQueries(limit));
    }
}