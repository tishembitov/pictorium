package ru.tishembitov.pictorium.analytics;

import ru.tishembitov.pictorium.analytics.dto.SearchHistoryResponse;
import ru.tishembitov.pictorium.analytics.dto.TrendingQueryResponse;
import ru.tishembitov.pictorium.search.dto.SearchRequest;

import java.util.List;

public interface SearchAnalyticsService {

    void logSearch(SearchRequest request, long resultsCount, long took, String userId);

    List<TrendingQueryResponse> getTrendingQueries(int limit);

    List<SearchHistoryResponse> getUserSearchHistory(String userId, int limit);

    void deleteUserSearchHistory(String userId);
}