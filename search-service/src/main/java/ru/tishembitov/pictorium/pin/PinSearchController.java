package ru.tishembitov.pictorium.pin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.search.SearchCriteria;
import ru.tishembitov.pictorium.search.SearchResult;

import java.time.Instant;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/search/pins")
@RequiredArgsConstructor
@Slf4j
public class PinSearchController {

    private final PinSearchService pinSearchService;

    @GetMapping
    public ResponseEntity<SearchResult<PinSearchResult>> searchPins(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(required = false) String authorId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(defaultValue = "RELEVANCE") SearchCriteria.SortBy sortBy,
            @RequestParam(defaultValue = "DESC") SearchCriteria.SortOrder sortOrder,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "true") Boolean fuzzy,
            @RequestParam(defaultValue = "true") Boolean highlight,
            @RequestParam(defaultValue = "true") Boolean personalized
    ) {
        SearchCriteria criteria = SearchCriteria.builder()
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

        log.debug("Pin search: query='{}', tags={}, page={}, size={}", q, tags, page, size);

        return ResponseEntity.ok(pinSearchService.searchPins(criteria));
    }

    @PostMapping
    public ResponseEntity<SearchResult<PinSearchResult>> searchPinsPost(
            @Valid @RequestBody SearchCriteria criteria
    ) {
        return ResponseEntity.ok(pinSearchService.searchPins(criteria));
    }

    @GetMapping("/{pinId}/similar")
    public ResponseEntity<SearchResult<PinSearchResult>> findSimilarPins(
            @PathVariable String pinId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer limit
    ) {
        log.debug("Finding similar pins: pinId={}, limit={}", pinId, limit);
        return ResponseEntity.ok(pinSearchService.findSimilarPins(pinId, limit));
    }
}