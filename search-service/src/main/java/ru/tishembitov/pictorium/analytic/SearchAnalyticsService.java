package ru.tishembitov.pictorium.analytic;

import ru.tishembitov.pictorium.history.SearchHistoryResponse;
import ru.tishembitov.pictorium.search.SearchCriteria;

import java.util.List;

public interface SearchAnalyticsService {

    void logSearch(SearchCriteria request, long resultsCount, long took, String userId);

    List<TrendingQueryResponse> getTrendingQueries(int limit);

    List<SearchHistoryResponse> getUserSearchHistory(String userId, int limit);

    void deleteUserSearchHistory(String userId);
}