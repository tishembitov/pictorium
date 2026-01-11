package ru.tishembitov.pictorium.kafka;

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
public class ContentEvent {

    private String type;
    private String actorId;
    private String recipientId;
    private UUID pinId;
    private UUID commentId;
    private UUID secondaryRefId;
    private String previewText;
    private String previewImageId;

    @Builder.Default
    private Instant timestamp = Instant.now();
}