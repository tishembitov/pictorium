package ru.tishembitov.pictorium.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.tishembitov.pictorium.kafka.event.BaseEvent;
import ru.tishembitov.pictorium.kafka.event.ChatEvent;
import ru.tishembitov.pictorium.kafka.event.ContentEvent;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationAggregationStrategy {

    private final NotificationRepository notificationRepository;

    public Optional<Notification> findForAggregation(BaseEvent event, NotificationType type) {
        String recipientId = event.getRecipientId();

        return switch (type) {
            case NEW_MESSAGE -> {
                ChatEvent chatEvent = (ChatEvent) event;
                yield notificationRepository.findUnreadMessagesNotification(
                        recipientId,
                        chatEvent.getChatId()
                );
            }

            case PIN_LIKED, PIN_SAVED, PIN_COMMENTED -> {
                ContentEvent contentEvent = (ContentEvent) event;
                yield notificationRepository.findUnreadPinNotification(
                        recipientId,
                        type,
                        contentEvent.getPinId()
                );
            }

            case COMMENT_LIKED -> {
                ContentEvent contentEvent = (ContentEvent) event;
                yield notificationRepository.findUnreadCommentLikeNotification(
                        recipientId,
                        contentEvent.getCommentId()
                );
            }

            case COMMENT_REPLIED -> {
                ContentEvent contentEvent = (ContentEvent) event;
                UUID parentCommentId = contentEvent.getSecondaryRefId();
                if (parentCommentId == null) {
                    yield Optional.empty();
                }
                yield notificationRepository.findUnreadRepliesNotification(
                        recipientId,
                        parentCommentId
                );
            }

            case USER_FOLLOWED -> {
                yield notificationRepository.findUnreadFollowsNotification(recipientId);
            }
        };
    }

    public boolean shouldAggregate(Notification existing, BaseEvent event, NotificationType type) {
        if (existing == null) {
            return false;
        }

        String actorId = event.getActorId();

        if (type == NotificationType.USER_FOLLOWED) {
            boolean alreadyInNotification = notificationRepository
                    .isActorAlreadyInFollowNotification(event.getRecipientId(), actorId);

            if (alreadyInNotification) {
                log.debug("Actor {} already in follow notification for {}, skipping",
                        actorId, event.getRecipientId());
                return false;
            }
            return true;
        }

        return existing.canAggregateWith(actorId, type);
    }

    public boolean isDuplicate(Notification existing, BaseEvent event, NotificationType type) {
        if (existing == null) {
            return false;
        }

        if (type == NotificationType.USER_FOLLOWED) {
            return notificationRepository.isActorAlreadyInFollowNotification(
                    event.getRecipientId(),
                    event.getActorId()
            );
        }

        return false;
    }

    public UUID getSecondaryRefId(BaseEvent event, NotificationType type) {
        if (event instanceof ChatEvent chatEvent) {
            return chatEvent.getMessageId();
        }

        if (event instanceof ContentEvent contentEvent) {
            if (type == NotificationType.COMMENT_REPLIED) {
                return contentEvent.getSecondaryRefId();
            }
            return contentEvent.getCommentId();
        }

        return null;
    }
}