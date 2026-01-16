package ru.tishembitov.pictorium.user;

import ru.tishembitov.pictorium.search.SearchCriteria;
import ru.tishembitov.pictorium.search.SearchResult;

public interface UserSearchService {

    SearchResult<UserSearchResult> searchUsers(SearchCriteria criteria);

    InternalSearchResult<UserSearchResult> searchUsersInternal(SearchCriteria criteria, int from, int size);

    record InternalSearchResult<T>(java.util.List<T> results, long totalHits) {}
}