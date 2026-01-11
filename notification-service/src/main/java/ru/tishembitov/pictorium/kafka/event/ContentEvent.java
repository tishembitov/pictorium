package ru.tishembitov.pictorium.kafka.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContentEvent extends BaseEvent {

    private UUID pinId;
    private UUID commentId;
    private UUID secondaryRefId;  // parentCommentId for replies

    @Override
    public UUID getReferenceId() {
        return pinId != null ? pinId : commentId;
    }
}