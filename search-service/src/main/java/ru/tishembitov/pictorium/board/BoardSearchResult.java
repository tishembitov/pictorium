package ru.tishembitov.pictorium.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardSearchResult {

    private String id;
    private String title;
    private String userId;
    private String username;
    private Integer pinCount;
    private String previewImageId;

    private Instant createdAt;

    private Map<String, List<String>> highlights;
    private Float score;
}