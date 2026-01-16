package ru.tishembitov.pictorium.board;

import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class BoardSearchMapper {

    public BoardSearchResult toSearchResult(Hit<BoardDocument> hit) {
        BoardDocument doc = hit.source();
        if (doc == null) return null;

        Map<String, List<String>> highlights = new HashMap<>();
        if (hit.highlight() != null) {
            highlights.putAll(hit.highlight());
        }

        return BoardSearchResult.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .userId(doc.getUserId())
                .username(doc.getUsername())
                .pinCount(doc.getPinCount())
                .previewImageId(doc.getPreviewImageId())
                .createdAt(doc.getCreatedAt())
                .highlights(highlights)
                .score(hit.score() != null ? hit.score().floatValue() : null)
                .build();
    }

    public List<BoardSearchResult> toSearchResultList(List<Hit<BoardDocument>> hits) {
        return hits.stream()
                .map(this::toSearchResult)
                .filter(Objects::nonNull)
                .toList();
    }
}