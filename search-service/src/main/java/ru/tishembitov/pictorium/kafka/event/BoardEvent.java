package ru.tishembitov.pictorium.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardEvent {

    private String type;
    private String actorId;
    private String actorUsername;

    private UUID boardId;
    private String boardTitle;
    private String previewImageId;
    private Integer pinCount;

    @Builder.Default
    private Instant timestamp = Instant.now();
}