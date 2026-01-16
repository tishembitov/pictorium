package ru.tishembitov.pictorium.history;

import java.util.List;

public interface SearchHistoryService {

    List<SearchHistoryResponse> getUserSearchHistory(String userId, int limit);

    void deleteUserSearchHistory(String userId);
}