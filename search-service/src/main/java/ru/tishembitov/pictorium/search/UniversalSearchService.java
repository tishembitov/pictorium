package ru.tishembitov.pictorium.search;

import java.util.List;

public interface UniversalSearchService {

    UniversalSearchResponse searchAll(SearchCriteria request);

    List<String> getTrendingSearches(int limit);
}