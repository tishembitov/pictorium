package ru.tishembitov.pictorium.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.tishembitov.pictorium.message.MessageType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEvent {

    private ChatEventType type;
    private UUID chatId;
    private UUID messageId;
    private String senderId;
    private String receiverId;
    private String content;
    private MessageType messageType;

    @Builder.Default
    private Instant timestamp = Instant.now();
}