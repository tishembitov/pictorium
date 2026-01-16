package ru.tishembitov.pictorium.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse<T> {

    private List<T> results;

    private long totalHits;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    private boolean hasNext;
    private boolean hasPrevious;

    private long took; // время выполнения в ms

    private String query;

    // Агрегации (для фасетного поиска)
    private Aggregations aggregations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Aggregations {
        private List<TagCount> topTags;
        private List<AuthorCount> topAuthors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagCount {
        private String tag;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorCount {
        private String authorId;
        private String authorUsername;
        private long count;
    }
}