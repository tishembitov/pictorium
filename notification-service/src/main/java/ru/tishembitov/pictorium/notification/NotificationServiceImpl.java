package ru.tishembitov.pictorium.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.counter.UnreadCounterService;
import ru.tishembitov.pictorium.exception.NotificationNotFoundException;
import ru.tishembitov.pictorium.kafka.event.BaseEvent;
import ru.tishembitov.pictorium.kafka.event.ChatEvent;
import ru.tishembitov.pictorium.kafka.event.ContentEvent;
import ru.tishembitov.pictorium.kafka.event.UserEvent;
import ru.tishembitov.pictorium.sse.SseEmitterManager;
import ru.tishembitov.pictorium.sse.SseEvent;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SseEmitterManager sseEmitterManager;
    private final UnreadCounterService unreadCounterService;

    @Override
    public void createAndSendNotification(BaseEvent event) {
        // Не отправляем уведомление самому себе
        if (event.getActorId().equals(event.getRecipientId())) {
            log.debug("Skipping self-notification for user {}", event.getActorId());
            return;
        }

        // Проверяем дедупликацию
        if (isDuplicate(event)) {
            log.debug("Duplicate notification detected, skipping: {}", event);
            return;
        }

        Notification notification = buildNotification(event);
        Notification saved = notificationRepository.save(notification);

        // Инкремент счетчика
        unreadCounterService.increment(event.getRecipientId());

        // Отправляем через SSE
        NotificationResponse response = notificationMapper.toResponse(saved);
        sseEmitterManager.sendToUser(
                event.getRecipientId(),
                SseEvent.notification(response)
        );

        log.info("Notification created and sent: type={}, recipient={}, actor={}",
                event.getType(), event.getRecipientId(), event.getActorId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        String userId = SecurityUtils.requireCurrentUserId();
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyUnreadNotifications(Pageable pageable) {
        String userId = SecurityUtils.requireCurrentUserId();
        return notificationRepository
                .findByRecipientIdAndStatusOrderByCreatedAtDesc(
                        userId, NotificationStatus.UNREAD, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        String userId = SecurityUtils.requireCurrentUserId();

        // Сначала пробуем из Redis
        Long cachedCount = unreadCounterService.getCount(userId);
        if (cachedCount != null) {
            return cachedCount;
        }

        // Fallback на БД
        long count = notificationRepository.countByRecipientIdAndStatus(
                userId, NotificationStatus.UNREAD);

        // Кэшируем результат
        unreadCounterService.setCount(userId, count);

        return count;
    }

    @Override
    public int markAllAsRead() {
        String userId = SecurityUtils.requireCurrentUserId();

        int updated = notificationRepository.markAllAsRead(
                userId,
                NotificationStatus.UNREAD,
                NotificationStatus.READ,
                Instant.now()
        );

        if (updated > 0) {
            unreadCounterService.reset(userId);

            // Уведомляем клиента об обновлении счетчика
            sseEmitterManager.sendToUser(userId, SseEvent.unreadUpdate(0));
        }

        log.info("Marked {} notifications as read for user {}", updated, userId);
        return updated;
    }

    @Override
    public int markAsRead(List<UUID> ids) {
        String userId = SecurityUtils.requireCurrentUserId();

        int updated = notificationRepository.markAsRead(
                ids, userId, NotificationStatus.READ, Instant.now()
        );

        if (updated > 0) {
            unreadCounterService.decrement(userId, updated);

            long newCount = getUnreadCount();
            sseEmitterManager.sendToUser(userId, SseEvent.unreadUpdate(newCount));
        }

        log.info("Marked {} notifications as read for user {}", updated, userId);
        return updated;
    }

    @Override
    public void delete(UUID id) {
        String userId = SecurityUtils.requireCurrentUserId();

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification not found: " + id));

        if (!notification.getRecipientId().equals(userId)) {
            throw new NotificationNotFoundException("Notification not found: " + id);
        }

        if (notification.getStatus() == NotificationStatus.UNREAD) {
            unreadCounterService.decrement(userId, 1);
        }

        notificationRepository.delete(notification);
        log.info("Notification {} deleted by user {}", id, userId);
    }

    /**
     * Очистка старых уведомлений (запускается каждый день)
     */
    @Scheduled(cron = "0 0 3 * * *") // 3:00 AM every day
    public void cleanupOldNotifications() {
        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteOlderThan(threshold);
        log.info("Cleaned up {} old notifications", deleted);
    }

    private Notification buildNotification(BaseEvent event) {
        Notification.NotificationBuilder builder = Notification.builder()
                .recipientId(event.getRecipientId())
                .actorId(event.getActorId())
                .type(mapEventType(event))
                .referenceId(event.getReferenceId())
                .previewText(event.getPreviewText())
                .previewImageId(event.getPreviewImageId());

        if (event instanceof ChatEvent chatEvent) {
            builder.secondaryRefId(chatEvent.getMessageId());
        } else if (event instanceof ContentEvent contentEvent) {
            builder.secondaryRefId(contentEvent.getSecondaryRefId());
        }

        return builder.build();
    }

    private NotificationType mapEventType(BaseEvent event) {
        return switch (event.getType()) {
            case "NEW_MESSAGE" -> NotificationType.NEW_MESSAGE;
            case "PIN_LIKED" -> NotificationType.PIN_LIKED;
            case "PIN_COMMENTED" -> NotificationType.PIN_COMMENTED;
            case "PIN_SAVED" -> NotificationType.PIN_SAVED;
            case "COMMENT_LIKED" -> NotificationType.COMMENT_LIKED;
            case "COMMENT_REPLIED" -> NotificationType.COMMENT_REPLIED;
            case "USER_FOLLOWED" -> NotificationType.USER_FOLLOWED;
            case "USER_MENTIONED" -> NotificationType.USER_MENTIONED;
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getType());
        };
    }

    private boolean isDuplicate(BaseEvent event) {
        if (event.getReferenceId() == null) {
            return false;
        }

        return notificationRepository.existsByRecipientIdAndActorIdAndTypeAndReferenceId(
                event.getRecipientId(),
                event.getActorId(),
                mapEventType(event),
                event.getReferenceId()
        );
    }
}