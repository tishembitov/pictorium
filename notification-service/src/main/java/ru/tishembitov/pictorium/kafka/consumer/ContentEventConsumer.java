package ru.tishembitov.pictorium.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.tishembitov.pictorium.kafka.event.ContentEvent;
import ru.tishembitov.pictorium.notification.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topics.content-events:content-events}",
            groupId = "notification-service",
            properties = {"spring.json.value.default.type=ru.tishembitov.pictorium.kafka.event.ContentEvent"}
    )
    public void consume(ContentEvent event) {
        log.info("Received content event: type={}, pinId={}, actor={}, recipient={}",
                event.getType(), event.getPinId(), event.getActorId(), event.getRecipientId());

        try {
            notificationService.createAndSendNotification(event);
        } catch (Exception e) {
            log.error("Error processing content event", e);
        }
    }
}