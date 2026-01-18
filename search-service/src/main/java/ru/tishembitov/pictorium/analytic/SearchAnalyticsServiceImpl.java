package ru.tishembitov.pictorium.analytic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.history.SearchHistoryResponse;
import ru.tishembitov.pictorium.history.SearchHistoryDocument;
import ru.tishembitov.pictorium.history.SearchHistoryRepository;
import ru.tishembitov.pictorium.search.SearchCriteria;


import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsServiceImpl implements SearchAnalyticsService {

    private final SearchAnalyticsRepository analyticsRepository;
    private final SearchHistoryRepository historyRepository;
    private final TrendingQueryRepository trendingRepository;

    @Value("${index.search-analytics.name:search_analytics}")
    private String analyticsIndex;

    @Override
    @Async
    public void logSearch(SearchCriteria request, long resultsCount, long took, String userId) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return;
        }

        try {
            String normalizedQuery = normalizeQuery(request.getQuery());

            SearchAnalyticsDocument analytics = SearchAnalyticsDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .query(request.getQuery())
                    .normalizedQuery(normalizedQuery)
                    .userId(userId)
                    .searchType(getSearchType(request))
                    .resultsCount(resultsCount)
                    .took(took)
                    .hasResults(resultsCount > 0)
                    .timestamp(Instant.now())
                    .build();

            analyticsRepository.save(analytics);

            if (userId != null) {
                updateUserHistory(userId, request.getQuery(), normalizedQuery, request);
            }

            updateTrendingQuery(normalizedQuery, request.getQuery());

            log.debug("Search logged: query='{}', results={}, took={}ms",
                    request.getQuery(), resultsCount, took);

        } catch (Exception e) {
            log.error("Failed to log search analytics", e);
        }
    }

    @Override
    public List<TrendingQueryResponse> getTrendingQueries(int limit) {
        return trendingRepository.findAllByOrderByTrendingScoreDesc(PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(doc -> TrendingQueryResponse.builder()
                        .query(doc.getQuery())
                        .searchCount(doc.getSearchCount())
                        .build())
                .toList();
    }

    @Override
    public List<SearchHistoryResponse> getUserSearchHistory(String userId, int limit) {
        return historyRepository.findByUserIdOrderByLastSearchedAtDesc(
                        userId, PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(doc -> SearchHistoryResponse.builder()
                        .query(doc.getQuery())
                        .searchType(doc.getSearchType())
                        .searchCount(doc.getSearchCount())
                        .lastSearchedAt(doc.getLastSearchedAt())
                        .build())
                .toList();
    }

    @Override
    public void deleteUserSearchHistory(String userId) {
        historyRepository.deleteByUserId(userId);
        log.info("Search history deleted for user: {}", userId);
    }

    // === Private methods ===

    private void updateUserHistory(String userId, String query, String normalizedQuery,
                                   SearchCriteria request) {
        Optional<SearchHistoryDocument> existingOpt =
                historyRepository.findByUserIdAndNormalizedQuery(userId, normalizedQuery);

        if (existingOpt.isPresent()) {
            SearchHistoryDocument existing = existingOpt.get();
            existing.setSearchCount(existing.getSearchCount() + 1);
            existing.setLastSearchedAt(Instant.now());
            historyRepository.save(existing);
        } else {
            SearchHistoryDocument history = SearchHistoryDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .query(query)
                    .normalizedQuery(normalizedQuery)
                    .searchType(getSearchType(request))
                    .searchCount(1)
                    .lastSearchedAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();

            historyRepository.save(history);
        }
    }

    private void updateTrendingQuery(String normalizedQuery, String originalQuery) {
        Optional<TrendingQueryDocument> existingOpt =
                trendingRepository.findByNormalizedQuery(normalizedQuery);

        if (existingOpt.isPresent()) {
            TrendingQueryDocument existing = existingOpt.get();
            existing.setSearchCount(existing.getSearchCount() + 1);
            existing.setLastUpdated(Instant.now());
            trendingRepository.save(existing);
        } else {
            TrendingQueryDocument trending = TrendingQueryDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .query(originalQuery)
                    .normalizedQuery(normalizedQuery)
                    .searchCount(1L)
                    .uniqueUsers(1L)
                    .trendingScore(1.0)
                    .lastUpdated(Instant.now())
                    .createdAt(Instant.now())
                    .build();

            trendingRepository.save(trending);
        }
    }

    private String normalizeQuery(String query) {
        if (query == null) return "";
        return query.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-zа-яё0-9\\s]", "");
    }

    private String getSearchType(SearchCriteria request) {
        if (request.getTypes() == null || request.getTypes().isEmpty()) {
            return "ALL";
        }
        return request.getTypes().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}