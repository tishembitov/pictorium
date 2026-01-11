package ru.tishembitov.pictorium.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.tishembitov.pictorium.kafka.event.UserEvent;
import ru.tishembitov.pictorium.notification.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topics.user-events:user-events}",
            groupId = "notification-service",
            properties = {"spring.json.value.default.type=ru.tishembitov.pictorium.kafka.event.UserEvent"}
    )
    public void consume(UserEvent event) {
        log.info("Received user event: type={}, actor={}, recipient={}",
                event.getType(), event.getActorId(), event.getRecipientId());

        try {
            notificationService.createAndSendNotification(event);
        } catch (Exception e) {
            log.error("Error processing user event", e);
        }
    }
}