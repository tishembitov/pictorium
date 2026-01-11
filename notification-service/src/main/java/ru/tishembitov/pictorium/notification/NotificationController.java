package ru.tishembitov.pictorium.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Получить все уведомления
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.getMyNotifications(pageable));
    }

    /**
     * Получить непрочитанные уведомления
     */
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.getMyUnreadNotifications(pageable));
    }

    /**
     * Получить количество непрочитанных
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount()));
    }

    /**
     * Пометить все как прочитанные
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllAsRead()));
    }

    /**
     * Пометить конкретные как прочитанные
     */
    @PatchMapping("/read")
    public ResponseEntity<Map<String, Integer>> markAsRead(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAsRead(ids)));
    }

    /**
     * Удалить уведомление
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}