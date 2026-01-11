package ru.tishembitov.pictorium.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tishembitov.pictorium.kafka.event.BaseEvent;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    /**
     * Создать и отправить уведомление
     */
    void createAndSendNotification(BaseEvent event);

    /**
     * Получить уведомления текущего пользователя
     */
    Page<NotificationResponse> getMyNotifications(Pageable pageable);

    /**
     * Получить непрочитанные уведомления
     */
    Page<NotificationResponse> getMyUnreadNotifications(Pageable pageable);

    /**
     * Получить количество непрочитанных
     */
    long getUnreadCount();

    /**
     * Пометить все как прочитанные
     */
    int markAllAsRead();

    /**
     * Пометить конкретные как прочитанные
     */
    int markAsRead(List<UUID> ids);

    /**
     * Удалить уведомление
     */
    void delete(UUID id);
}