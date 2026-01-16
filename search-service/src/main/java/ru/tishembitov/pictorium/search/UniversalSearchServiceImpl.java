package ru.tishembitov.pictorium.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.analytic.SearchAnalyticsService;
import ru.tishembitov.pictorium.board.BoardSearchService;
import ru.tishembitov.pictorium.exception.SearchException;
import ru.tishembitov.pictorium.personalization.PersonalizationBoosts;
import ru.tishembitov.pictorium.personalization.PersonalizationService;
import ru.tishembitov.pictorium.pin.PinDocument;
import ru.tishembitov.pictorium.pin.PinSearchService;
import ru.tishembitov.pictorium.user.UserSearchService;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniversalSearchServiceImpl implements UniversalSearchService {

    private final PinSearchService pinSearchService;
    private final UserSearchService userSearchService;
    private final BoardSearchService boardSearchService;
    private final PersonalizationService personalizationService;
    private final SearchAnalyticsService analyticsService;
    private final ElasticsearchClient elasticsearchClient;

    @Value("${index.pins.name:pins}")
    private String pinsIndex;

    @Override
    public UniversalSearchResponse searchAll(SearchCriteria request) {
        long startTime = System.currentTimeMillis();

        int limitPerType = Math.min(request.getSize(), 10);

        try {
            String userId = SecurityUtils.getCurrentUserId().orElse(null);
            PersonalizationBoosts boosts = getBoostsIfEnabled(request, userId);

            var pinsFuture = pinSearchService.searchPinsInternal(request, 0, limitPerType, boosts);
            var usersFuture = userSearchService.searchUsersInternal(request, 0, limitPerType);
            var boardsFuture = boardSearchService.searchBoardsInternal(request, 0, limitPerType);

            long took = System.currentTimeMillis() - startTime;

            long totalResults = pinsFuture.totalHits() + usersFuture.totalHits() + boardsFuture.totalHits();
            analyticsService.logSearch(request, totalResults, took, userId);

            log.info("Universal search completed: query='{}', took={}ms", request.getQuery(), took);

            return UniversalSearchResponse.builder()
                    .pins(pinsFuture.results())
                    .users(usersFuture.results())
                    .boards(boardsFuture.results())
                    .totalPins(pinsFuture.totalHits())
                    .totalUsers(usersFuture.totalHits())
                    .totalBoards(boardsFuture.totalHits())
                    .hasMorePins(pinsFuture.totalHits() > limitPerType)
                    .hasMoreUsers(usersFuture.totalHits() > limitPerType)
                    .hasMoreBoards(boardsFuture.totalHits() > limitPerType)
                    .took(took)
                    .query(request.getQuery())
                    .build();

        } catch (Exception e) {
            log.error("Universal search failed: query='{}'", request.getQuery(), e);
            throw new SearchException("Search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getTrendingSearches(int limit) {
        try {
            SearchResponse<PinDocument> response = elasticsearchClient.search(
                    s -> s.index(pinsIndex)
                            .size(0)
                            .aggregations("top_tags", a -> a
                                    .terms(t -> t.field("tags").size(limit))),
                    PinDocument.class
            );

            var tagsAgg = response.aggregations().get("top_tags");
            if (tagsAgg != null && tagsAgg.isSterms()) {
                return tagsAgg.sterms().buckets().array().stream()
                        .map(b -> b.key().stringValue())
                        .toList();
            }

            return Collections.emptyList();

        } catch (IOException e) {
            log.error("Failed to get trending searches", e);
            return Collections.emptyList();
        }
    }

    private PersonalizationBoosts getBoostsIfEnabled(SearchCriteria request, String userId) {
        if (Boolean.FALSE.equals(request.getPersonalized()) || userId == null) {
            return PersonalizationBoosts.empty();
        }
        return personalizationService.getBoostsForUser(userId);
    }
}