package ru.tishembitov.pictorium.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentEventPublisher {

    private final KafkaTemplate<String, ContentEvent> kafkaTemplate;

    @Value("${kafka.topics.content-events:content-events}")
    private String contentEventsTopic;

    public void publish(ContentEvent event) {
        String key = event.getRecipientId();

        kafkaTemplate.send(contentEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish content event: {}", event, ex);
                    } else {
                        log.debug("Content event published: type={}, recipient={}",
                                event.getType(), event.getRecipientId());
                    }
                });
    }
}