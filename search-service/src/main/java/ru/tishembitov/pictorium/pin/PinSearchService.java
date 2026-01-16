package ru.tishembitov.pictorium.pin;

import ru.tishembitov.pictorium.personalization.PersonalizationBoosts;
import ru.tishembitov.pictorium.search.SearchCriteria;
import ru.tishembitov.pictorium.search.SearchResult;

public interface PinSearchService {

    SearchResult<PinSearchResult> searchPins(SearchCriteria criteria);

    SearchResult<PinSearchResult> searchPins(SearchCriteria criteria, String userId);

    SearchResult<PinSearchResult> findSimilarPins(String pinId, int limit);

    InternalSearchResult<PinSearchResult> searchPinsInternal(
            SearchCriteria criteria,
            int from,
            int size,
            PersonalizationBoosts boosts
    );

    record InternalSearchResult<T>(java.util.List<T> results, long totalHits) {}
}