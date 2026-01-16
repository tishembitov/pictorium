// PersonalizationEventConsumer.java
package ru.tishembitov.pictorium.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.tishembitov.pictorium.kafka.event.ContentEvent;
import ru.tishembitov.pictorium.kafka.event.UserEvent;
import ru.tishembitov.pictorium.personalization.PersonalizationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PersonalizationEventConsumer {

    private final PersonalizationService personalizationService;

    @KafkaListener(
            topics = "${kafka.topics.content-events:content-events}",
            groupId = "search-service-personalization",
            properties = {"spring.json.value.default.type=ru.tishembitov.pictorium.kafka.event.ContentEvent"}
    )
    public void consumeContentEvent(ContentEvent event) {
        try {
            switch (event.getType()) {
                case "PIN_LIKED" -> personalizationService.onPinLiked(
                        event.getActorId(),
                        event.getPinId() != null ? event.getPinId().toString() : null,
                        event.getPinTags(),
                        event.getRecipientId() // author of the pin
                );
                case "PIN_SAVED" -> personalizationService.onPinSaved(
                        event.getActorId(),
                        event.getPinId() != null ? event.getPinId().toString() : null,
                        event.getPinTags(),
                        event.getRecipientId()
                );
            }
        } catch (Exception e) {
            log.error("Error processing content event for personalization", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.user-events:user-events}",
            groupId = "search-service-personalization",
            properties = {"spring.json.value.default.type=ru.tishembitov.pictorium.kafka.event.UserEvent"}
    )
    public void consumeUserEvent(UserEvent event) {
        try {
            if ("USER_FOLLOWED".equals(event.getType())) {
                personalizationService.onUserFollowed(
                        event.getActorId(),
                        event.getRecipientId()
                );
            }
        } catch (Exception e) {
            log.error("Error processing user event for personalization", e);
        }
    }
}