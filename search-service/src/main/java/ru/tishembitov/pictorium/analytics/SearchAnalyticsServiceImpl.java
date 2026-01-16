package ru.tishembitov.pictorium.analytics;

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
import ru.tishembitov.pictorium.analytics.dto.SearchHistoryResponse;
import ru.tishembitov.pictorium.analytics.dto.TrendingQueryResponse;
import ru.tishembitov.pictorium.document.SearchAnalyticsDocument;
import ru.tishembitov.pictorium.document.SearchHistoryDocument;
import ru.tishembitov.pictorium.document.TrendingQueryDocument;
import ru.tishembitov.pictorium.repository.SearchAnalyticsRepository;
import ru.tishembitov.pictorium.repository.SearchHistoryRepository;
import ru.tishembitov.pictorium.repository.TrendingQueryRepository;
import ru.tishembitov.pictorium.search.dto.SearchRequest;

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
    private final ElasticsearchClient elasticsearchClient;

    @Value("${index.search-analytics.name:search_analytics}")
    private String analyticsIndex;

    @Override
    @Async
    public void logSearch(SearchRequest request, long resultsCount, long took, String userId) {
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

    @Scheduled(cron = "0 0 * * * *") // Каждый час
    public void recalculateTrendingScores() {
        log.info("Recalculating trending scores...");

        Instant now = Instant.now();
        Instant hourAgo = now.minus(1, ChronoUnit.HOURS);

        try {
            SearchResponse<SearchAnalyticsDocument> hourlyResponse = elasticsearchClient.search(s -> s
                            .index(analyticsIndex)
                            .size(0)
                            .query(q -> q.range(r -> r
                                    .date(d -> d
                                            .field("timestamp")
                                            .gte(hourAgo.toString())
                                    )
                            ))
                            .aggregations("queries", a -> a
                                    .terms(t -> t.field("normalizedQuery").size(100))
                                    .aggregations("unique_users", sub -> sub
                                            .cardinality(c -> c.field("userId"))
                                    )
                            ),
                    SearchAnalyticsDocument.class
            );

            var queriesAgg = hourlyResponse.aggregations().get("queries");
            if (queriesAgg != null && queriesAgg.isSterms()) {
                for (StringTermsBucket bucket : queriesAgg.sterms().buckets().array()) {
                    String query = bucket.key().stringValue();
                    long hourlyCount = bucket.docCount();
                    long uniqueUsers = bucket.aggregations().get("unique_users").cardinality().value();

                    double score = hourlyCount * Math.log(uniqueUsers + 1) * 2.0;

                    trendingRepository.findByNormalizedQuery(query)
                            .ifPresent(doc -> {
                                doc.setTrendingScore(score);
                                doc.setLastUpdated(now);
                                trendingRepository.save(doc);
                            });
                }
            }

            log.info("Trending scores recalculated");

        } catch (IOException e) {
            log.error("Failed to recalculate trending scores", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *") // Каждый день в 3:00
    public void cleanupOldAnalytics() {
        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);

        try {
            elasticsearchClient.deleteByQuery(d -> d
                    .index(analyticsIndex)
                    .query(q -> q.range(r -> r
                            .date(dt -> dt
                                    .field("timestamp")
                                    .lt(threshold.toString())
                            )
                    ))
            );

            log.info("Old analytics cleaned up (before {})", threshold);

        } catch (IOException e) {
            log.error("Failed to cleanup old analytics", e);
        }
    }

    private void updateUserHistory(String userId, String query, String normalizedQuery,
                                   SearchRequest request) {
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

    private String getSearchType(SearchRequest request) {
        if (request.getTypes() == null || request.getTypes().isEmpty()) {
            return "ALL";
        }
        return request.getTypes().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }
}