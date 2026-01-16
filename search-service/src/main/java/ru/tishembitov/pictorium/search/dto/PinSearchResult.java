package ru.tishembitov.pictorium.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinSearchResult {

    private String id;
    private String title;
    private String description;
    private Set<String> tags;

    private String authorId;
    private String authorUsername;

    private String imageId;
    private String thumbnailId;

    private Integer likeCount;
    private Integer saveCount;
    private Integer commentCount;

    private Integer originalWidth;
    private Integer originalHeight;

    private Instant createdAt;

    // Highlight matches
    private Map<String, List<String>> highlights;

    // Search score
    private Float score;
}