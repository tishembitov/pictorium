package ru.tishembitov.pictorium.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.tishembitov.pictorium.kafka.event.ChatEvent;
import ru.tishembitov.pictorium.notification.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topics.chat-events:chat-events}",
            groupId = "notification-service",
            properties = {"spring.json.value.default.type=ru.tishembitov.pictorium.kafka.event.ChatEvent"}
    )
    public void consume(ChatEvent event) {
        log.info("Received chat event: type={}, chatId={}, sender={}, recipient={}",
                event.getType(), event.getChatId(), event.getActorId(), event.getRecipientId());

        try {
            // Обрабатываем только NEW_MESSAGE
            if ("NEW_MESSAGE".equals(event.getType())) {
                notificationService.createAndSendNotification(event);
            }
        } catch (Exception e) {
            log.error("Error processing chat event", e);
        }
    }
}