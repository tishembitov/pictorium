package ru.tishembitov.pictorium.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult<T> {

    private List<T> results;

    private long totalHits;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    private boolean hasNext;
    private boolean hasPrevious;

    private long took;

    private String query;

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