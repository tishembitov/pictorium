package ru.tishembitov.pictorium.search;

import ru.tishembitov.pictorium.search.dto.*;

import java.util.List;

public interface SearchService {

    UniversalSearchResponse searchAll(SearchRequest request);

    SearchResponse<PinSearchResult> searchPins(SearchRequest request);
    SearchResponse<UserSearchResult> searchUsers(SearchRequest request);
    SearchResponse<BoardSearchResult> searchBoards(SearchRequest request);

    SearchResponse<PinSearchResult> findSimilarPins(String pinId, int limit);

    SuggestResponse suggest(String query, int limit);

    List<String> getTrendingSearches(int limit);
}