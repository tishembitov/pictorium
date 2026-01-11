package ru.tishembitov.pictorium.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatEvent extends BaseEvent {

    private UUID chatId;
    private UUID messageId;
    private String messageType;

    @Override
    public UUID getReferenceId() {
        return chatId;
    }
}