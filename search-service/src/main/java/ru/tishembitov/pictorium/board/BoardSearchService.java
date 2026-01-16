package ru.tishembitov.pictorium.board;

import ru.tishembitov.pictorium.search.SearchCriteria;
import ru.tishembitov.pictorium.search.SearchResult;

public interface BoardSearchService {

    SearchResult<BoardSearchResult> searchBoards(SearchCriteria criteria);

    InternalSearchResult<BoardSearchResult> searchBoardsInternal(SearchCriteria criteria, int from, int size);

    record InternalSearchResult<T>(java.util.List<T> results, long totalHits) {}
}