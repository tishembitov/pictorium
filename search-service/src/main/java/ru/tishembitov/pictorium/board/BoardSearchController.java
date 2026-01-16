package ru.tishembitov.pictorium.board;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.search.SearchCriteria;
import ru.tishembitov.pictorium.search.SearchResult;

@RestController
@RequestMapping("/api/v1/search/boards")
@RequiredArgsConstructor
@Slf4j
public class BoardSearchController {

    private final BoardSearchService boardSearchService;

    @GetMapping
    public ResponseEntity<SearchResult<BoardSearchResult>> searchBoards(
            @RequestParam String q,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "RELEVANCE") SearchCriteria.SortBy sortBy,
            @RequestParam(defaultValue = "DESC") SearchCriteria.SortOrder sortOrder,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "true") Boolean fuzzy,
            @RequestParam(defaultValue = "true") Boolean highlight
    ) {
        SearchCriteria criteria = SearchCriteria.builder()
                .query(q)
                .authorId(userId)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .page(page)
                .size(size)
                .fuzzy(fuzzy)
                .highlight(highlight)
                .build();

        log.debug("Board search: query='{}', page={}, size={}", q, page, size);

        return ResponseEntity.ok(boardSearchService.searchBoards(criteria));
    }
}