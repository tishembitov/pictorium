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
public class ContentEvent extends BaseEvent {

    private UUID pinId;
    private UUID commentId;
    private UUID secondaryRefId;  // parentCommentId for replies

    @Override
    public UUID getReferenceId() {
        return pinId != null ? pinId : commentId;
    }
}