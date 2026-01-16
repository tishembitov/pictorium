package ru.tishembitov.pictorium.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {

    @Size(min = 1, max = 200, message = "Query must be between 1 and 200 characters")
    private String query;

    private Set<SearchType> types;

    private Set<String> tags;

    private String authorId;

    private Instant createdFrom;

    private Instant createdTo;

    @Builder.Default
    private SortBy sortBy = SortBy.RELEVANCE;

    @Builder.Default
    private SortOrder sortOrder = SortOrder.DESC;

    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private Boolean fuzzy = true;

    @Builder.Default
    private Boolean highlight = true;

    @Builder.Default
    private Boolean personalized = true;

    public enum SearchType {
        PINS, USERS, BOARDS, ALL
    }

    public enum SortBy {
        RELEVANCE, RECENT, POPULAR, LIKES, SAVES
    }

    public enum SortOrder {
        ASC, DESC
    }
}