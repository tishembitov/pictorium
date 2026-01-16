package ru.tishembitov.pictorium.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.analytics.SearchAnalyticsService;
import ru.tishembitov.pictorium.analytics.dto.TrendingQueryResponse;
import ru.tishembitov.pictorium.search.dto.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;
    private final SearchAnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<UniversalSearchResponse> searchAll(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer size,
            @RequestParam(defaultValue = "true") Boolean fuzzy
    ) {
        SearchRequest request = SearchRequest.builder()
                .query(q)
                .size(size)
                .fuzzy(fuzzy)
                .build();

        log.debug("Universal search request: query='{}'", q);

        return ResponseEntity.ok(searchService.searchAll(request));
    }

    @GetMapping("/pins")
    public ResponseEntity<SearchResponse<PinSearchResult>> searchPins(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(required = false) String authorId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "RELEVANCE") SearchRequest.SortBy sortBy,
            @RequestParam(defaultValue = "DESC") SearchRequest.SortOrder sortOrder,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "true") Boolean fuzzy,
            @RequestParam(defaultValue = "true") Boolean highlight,
            @RequestParam(defaultValue = "true") Boolean personalized
    ) {
        SearchRequest request = SearchRequest.builder()
                .query(q)
                .tags(tags)
                .authorId(authorId)
                .createdFrom(createdFrom)
                .createdTo(createdTo)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .page(page)
                .size(size)
                .fuzzy(fuzzy)
                .highlight(highlight)
                .personalized(personalized)
                .build();

        log.debug("Pin search request: query='{}', tags={}, page={}, size={}",
                q, tags, page, size);

        return ResponseEntity.ok(searchService.searchPins(request));
    }

    @PostMapping("/pins")
    public ResponseEntity<SearchResponse<PinSearchResult>> searchPinsPost(
            @Valid @RequestBody SearchRequest request
    ) {
        return ResponseEntity.ok(searchService.searchPins(request));
    }

    @GetMapping("/users")
    public ResponseEntity<SearchResponse<UserSearchResult>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "RELEVANCE") SearchRequest.SortBy sortBy,
            @RequestParam(defaultValue = "DESC") SearchRequest.SortOrder sortOrder,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "true") Boolean fuzzy,
            @RequestParam(defaultValue = "true") Boolean highlight
    ) {
        SearchRequest request = SearchRequest.builder()
                .query(q)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .page(page)
                .size(size)
                .fuzzy(fuzzy)
                .highlight(highlight)
                .build();

        log.debug("User search request: query='{}', page={}, size={}", q, page, size);

        return ResponseEntity.ok(searchService.searchUsers(request));
    }

    @GetMapping("/boards")
    public ResponseEntity<SearchResponse<BoardSearchResult>> searchBoards(
            @RequestParam String q,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "RELEVANCE") SearchRequest.SortBy sortBy,
            @RequestParam(defaultValue = "DESC") SearchRequest.SortOrder sortOrder,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "true") Boolean fuzzy,
            @RequestParam(defaultValue = "true") Boolean highlight
    ) {
        SearchRequest request = SearchRequest.builder()
                .query(q)
                .authorId(userId)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .page(page)
                .size(size)
                .fuzzy(fuzzy)
                .highlight(highlight)
                .build();

        log.debug("Board search request: query='{}', page={}, size={}", q, page, size);

        return ResponseEntity.ok(searchService.searchBoards(request));
    }

    @GetMapping("/pins/{pinId}/similar")
    public ResponseEntity<SearchResponse<PinSearchResult>> findSimilarPins(
            @PathVariable String pinId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer limit
    ) {
        log.debug("Finding similar pins: pinId={}, limit={}", pinId, limit);
        return ResponseEntity.ok(searchService.findSimilarPins(pinId, limit));
    }

    @GetMapping("/suggest")
    public ResponseEntity<SuggestResponse> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) Integer limit
    ) {
        log.debug("Suggest request: query='{}', limit={}", q, limit);
        return ResponseEntity.ok(searchService.suggest(q, limit));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<TrendingQueryResponse>> getTrendingSearches(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer limit
    ) {
        return ResponseEntity.ok(analyticsService.getTrendingQueries(limit));
    }
}