package ru.tishembitov.pictorium.pin;

import ru.tishembitov.pictorium.kafka.event.ContentEvent;

import java.util.List;
import java.util.Optional;

public interface PinIndexService {

    void indexPin(ContentEvent event);

    void updatePin(ContentEvent event);

    void updatePinCounters(ContentEvent event);

    void deletePin(String pinId);

    Optional<PinDocument> findById(String pinId);

    void reindexAll(List<ContentEvent> pins);

    long count();
}