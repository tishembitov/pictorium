package ru.tishembitov.pictorium.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.tishembitov.pictorium.board.BoardIndexService;
import ru.tishembitov.pictorium.pin.PinIndexService;
import ru.tishembitov.pictorium.kafka.event.ContentEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentEventConsumer {

    private final PinIndexService pinIndexService;
    private final BoardIndexService boardIndexService;

    @KafkaListener(
            topics = "${kafka.topics.content-events:content-events}",
            groupId = "search-service",
            properties = {"spring.json.value.default.type=ru.tishembitov.pictorium.kafka.event.ContentEvent"}
    )
    public void consume(ContentEvent event) {
        log.info("Received content event: type={}, pinId={}, boardId={}",
                event.getType(), event.getPinId(), event.getBoardId());

        try {
            switch (event.getType()) {
                // Pin events
                case "PIN_CREATED" -> pinIndexService.indexPin(event);
                case "PIN_UPDATED" -> pinIndexService.updatePin(event);
                case "PIN_DELETED" -> pinIndexService.deletePin(event.getPinId().toString());
                case "PIN_LIKED", "PIN_SAVED", "PIN_COMMENTED" ->
                        pinIndexService.updatePinCounters(event);

                case "BOARD_CREATED" -> boardIndexService.indexBoard(mapToBoardEvent(event));
                case "BOARD_UPDATED" -> boardIndexService.updateBoard(mapToBoardEvent(event));
                case "BOARD_DELETED" -> boardIndexService.deleteBoard(event.getBoardId().toString());
                case "BOARD_PIN_ADDED", "BOARD_PIN_REMOVED" ->
                        boardIndexService.updateBoardPinCount(mapToBoardEvent(event));

                default -> log.debug("Ignoring content event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing content event: type={}", event.getType(), e);
        }
    }

    private ru.tishembitov.pictorium.kafka.event.BoardEvent mapToBoardEvent(ContentEvent event) {
        return ru.tishembitov.pictorium.kafka.event.BoardEvent.builder()
                .type(event.getType())
                .actorId(event.getActorId())
                .actorUsername(event.getActorUsername())
                .boardId(event.getBoardId())
                .boardTitle(event.getBoardTitle())
                .previewImageId(event.getPreviewImageId())
                .pinCount(event.getBoardPinCount())
                .timestamp(event.getTimestamp())
                .build();
    }
}