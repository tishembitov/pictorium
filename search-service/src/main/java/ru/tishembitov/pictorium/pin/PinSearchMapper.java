package ru.tishembitov.pictorium.pin;

import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class PinSearchMapper {

    public PinSearchResult toSearchResult(Hit<PinDocument> hit) {
        PinDocument doc = hit.source();
        if (doc == null) return null;

        Map<String, List<String>> highlights = new HashMap<>();
        if (hit.highlight() != null) {
            highlights.putAll(hit.highlight());
        }

        return PinSearchResult.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .tags(doc.getTags())
                .authorId(doc.getAuthorId())
                .authorUsername(doc.getAuthorUsername())
                .imageId(doc.getImageId())
                .thumbnailId(doc.getThumbnailId())
                .likeCount(doc.getLikeCount())
                .saveCount(doc.getSaveCount())
                .commentCount(doc.getCommentCount())
                .originalWidth(doc.getOriginalWidth())
                .originalHeight(doc.getOriginalHeight())
                .createdAt(doc.getCreatedAt())
                .highlights(highlights)
                .score(hit.score() != null ? hit.score().floatValue() : null)
                .build();
    }

    public List<PinSearchResult> toSearchResultList(List<Hit<PinDocument>> hits) {
        return hits.stream()
                .map(this::toSearchResult)
                .filter(Objects::nonNull)
                .toList();
    }
}