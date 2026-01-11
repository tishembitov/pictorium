package ru.tishembitov.pictorium.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatEventPublisher {

    private final KafkaTemplate<String, ChatEvent> kafkaTemplate;

    @Value("${kafka.topics.chat-events:chat-events}")
    private String chatEventsTopic;

    public void publish(ChatEvent event) {
        String key = event.getReceiverId(); // Partition by receiver for ordering

        CompletableFuture<SendResult<String, ChatEvent>> future =
                kafkaTemplate.send(chatEventsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish chat event: {}", event, ex);
            } else {
                log.debug("Chat event published: type={}, receiver={}, partition={}",
                        event.getType(),
                        event.getReceiverId(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}