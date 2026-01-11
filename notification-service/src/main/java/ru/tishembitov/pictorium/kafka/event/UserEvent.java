package ru.tishembitov.pictorium.kafka.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserEvent extends BaseEvent {

    @Override
    public UUID getReferenceId() {
        return null;
    }
}