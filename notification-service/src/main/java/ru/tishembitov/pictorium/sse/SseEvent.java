package ru.tishembitov.pictorium.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.tishembitov.pictorium.notification.NotificationResponse;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseEvent {

    private String type;
    private Object data;

    @Builder.Default
    private Instant timestamp = Instant.now();

    public static SseEvent notification(NotificationResponse notification) {
        return SseEvent.builder()
                .type("notification")
                .data(notification)
                .build();
    }

    public static SseEvent notificationUpdated(NotificationResponse notification) {
        return SseEvent.builder()
                .type("notification_updated")
                .data(notification)
                .build();
    }

    public static SseEvent unreadUpdate(long count) {
        return SseEvent.builder()
                .type("unread_update")
                .data(java.util.Map.of("count", count))
                .build();
    }

    public static SseEvent heartbeat() {
        return SseEvent.builder()
                .type("heartbeat")
                .data(java.util.Map.of("timestamp", Instant.now().toEpochMilli()))
                .build();
    }

    public static SseEvent connected(String userId) {
        return SseEvent.builder()
                .type("connected")
                .data(java.util.Map.of("userId", userId))
                .build();
    }
}