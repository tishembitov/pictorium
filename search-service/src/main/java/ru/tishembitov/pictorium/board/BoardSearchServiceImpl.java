package ru.tishembitov.pictorium.board;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.analytic.SearchAnalyticsService;
import ru.tishembitov.pictorium.exception.SearchException;
import ru.tishembitov.pictorium.search.SearchCriteria;
import ru.tishembitov.pictorium.search.SearchResult;
import ru.tishembitov.pictorium.search.SearchResultBuilder;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardSearchServiceImpl implements BoardSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final BoardSearchMapper boardMapper;
    private final SearchResultBuilder resultBuilder;
    private final SearchAnalyticsService analyticsService;

    @Value("${index.boards.name:boards}")
    private String boardsIndex;

    @Value("${search.highlight.pre-tag:<em>}")
    private String highlightPreTag;

    @Value("${search.highlight.post-tag:</em>}")
    private String highlightPostTag;

    @Value("${search.fuzzy.max-expansions:50}")
    private int fuzzyMaxExpansions;

    @Value("${search.fuzzy.prefix-length:2}")
    private int fuzzyPrefixLength;

    @Override
    public SearchResult<BoardSearchResult> searchBoards(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();

        try {
            InternalSearchResult<BoardSearchResult> result = searchBoardsInternal(
                    criteria,
                    criteria.getPage() * criteria.getSize(),
                    criteria.getSize()
            );

            long took = System.currentTimeMillis() - startTime;

            log.info("Board search completed: query='{}', hits={}, took={}ms",
                    criteria.getQuery(), result.totalHits(), took);

            String userId = SecurityUtils.getCurrentUserId().orElse(null);
            analyticsService.logSearch(criteria, result.totalHits(), took, userId);

            return resultBuilder.build(
                    result.results(),
                    result.totalHits(),
                    criteria.getPage(),
                    criteria.getSize(),
                    took,
                    criteria.getQuery()
            );

        } catch (Exception e) {
            log.error("Board search failed: query='{}'", criteria.getQuery(), e);
            throw new SearchException("Search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InternalSearchResult<BoardSearchResult> searchBoardsInternal(
            SearchCriteria criteria, int from, int size) {

        try {
            Query query = buildBoardQuery(criteria);

            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(boardsIndex)
                    .query(query)
                    .from(from)
                    .size(size);

            addSorting(searchBuilder, criteria);

            if (Boolean.TRUE.equals(criteria.getHighlight())) {
                addHighlighting(searchBuilder);
            }

            SearchResponse<BoardDocument> response = elasticsearchClient.search(
                    searchBuilder.build(),
                    BoardDocument.class
            );

            List<BoardSearchResult> results = boardMapper.toSearchResultList(response.hits().hits());

            long totalHits = response.hits().total() != null
                    ? response.hits().total().value()
                    : 0;

            return new InternalSearchResult<>(results, totalHits);

        } catch (IOException e) {
            throw new SearchException("Failed to search boards", e);
        }
    }

    private Query buildBoardQuery(SearchCriteria criteria) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
            Query textQuery;
            if (Boolean.TRUE.equals(criteria.getFuzzy())) {
                textQuery = MultiMatchQuery.of(m -> m
                        .query(criteria.getQuery())
                        .fields("title^3", "username^1")
                        .fuzziness("AUTO")
                        .prefixLength(fuzzyPrefixLength)
                        .maxExpansions(fuzzyMaxExpansions)
                        .type(TextQueryType.BestFields)
                )._toQuery();
            } else {
                textQuery = MultiMatchQuery.of(m -> m
                        .query(criteria.getQuery())
                        .fields("title^3", "username^1")
                        .type(TextQueryType.BestFields)
                )._toQuery();
            }
            boolBuilder.must(textQuery);
        }

        if (criteria.getAuthorId() != null) {
            boolBuilder.filter(TermQuery.of(t -> t
                    .field("userId")
                    .value(criteria.getAuthorId())
            )._toQuery());
        }

        return boolBuilder.build()._toQuery();
    }

    private void addSorting(SearchRequest.Builder builder, SearchCriteria criteria) {
        switch (criteria.getSortBy()) {
            case POPULAR -> builder.sort(s -> s.field(f -> f
                    .field("pinCount")
                    .order(SortOrder.Desc)));
            case RECENT -> builder.sort(s -> s.field(f -> f
                    .field("createdAt")
                    .order(SortOrder.Desc)));
            default -> builder.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }
    }

    private void addHighlighting(SearchRequest.Builder builder) {
        builder.highlight(h -> h
                .preTags(highlightPreTag)
                .postTags(highlightPostTag)
                .fields("title", HighlightField.of(hf -> hf.numberOfFragments(0)))
        );
    }
}