package ru.tishembitov.pictorium.search.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.elasticsearch.core.SearchHit;
import ru.tishembitov.pictorium.document.BoardDocument;
import ru.tishembitov.pictorium.document.PinDocument;
import ru.tishembitov.pictorium.document.UserDocument;
import ru.tishembitov.pictorium.search.dto.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SearchMapper {

    @Mapping(target = "highlights", ignore = true)
    @Mapping(target = "score", ignore = true)
    PinSearchResult toPinSearchResult(PinDocument document);

    default PinSearchResult toPinSearchResult(SearchHit<PinDocument> hit) {
        PinDocument doc = hit.getContent();
        PinSearchResult result = toPinSearchResult(doc);
        result.setScore(hit.getScore());
        result.setHighlights(hit.getHighlightFields());
        return result;
    }

    default List<PinSearchResult> toPinSearchResultList(List<SearchHit<PinDocument>> hits) {
        return hits.stream()
                .map(this::toPinSearchResult)
                .toList();
    }

    @Mapping(target = "highlights", ignore = true)
    @Mapping(target = "score", ignore = true)
    UserSearchResult toUserSearchResult(UserDocument document);

    default UserSearchResult toUserSearchResult(SearchHit<UserDocument> hit) {
        UserDocument doc = hit.getContent();
        UserSearchResult result = toUserSearchResult(doc);
        result.setScore(hit.getScore());
        result.setHighlights(hit.getHighlightFields());
        return result;
    }

    default List<UserSearchResult> toUserSearchResultList(List<SearchHit<UserDocument>> hits) {
        return hits.stream()
                .map(this::toUserSearchResult)
                .toList();
    }

    @Mapping(target = "highlights", ignore = true)
    @Mapping(target = "score", ignore = true)
    BoardSearchResult toBoardSearchResult(BoardDocument document);

    default BoardSearchResult toBoardSearchResult(SearchHit<BoardDocument> hit) {
        BoardDocument doc = hit.getContent();
        BoardSearchResult result = toBoardSearchResult(doc);
        result.setScore(hit.getScore());
        result.setHighlights(hit.getHighlightFields());
        return result;
    }

    default List<BoardSearchResult> toBoardSearchResultList(List<SearchHit<BoardDocument>> hits) {
        return hits.stream()
                .map(this::toBoardSearchResult)
                .toList();
    }

    default <T> SearchResponse<T> toSearchResponse(
            List<T> results,
            long totalHits,
            int page,
            int size,
            long took,
            String query
    ) {
        int totalPages = (int) Math.ceil((double) totalHits / size);

        return SearchResponse.<T>builder()
                .results(results)
                .totalHits(totalHits)
                .totalPages(totalPages)
                .currentPage(page)
                .pageSize(size)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .took(took)
                .query(query)
                .build();
    }
}