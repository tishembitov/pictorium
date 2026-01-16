package ru.tishembitov.pictorium.index.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.document.PinDocument;
import ru.tishembitov.pictorium.exception.IndexingException;
import ru.tishembitov.pictorium.index.PinIndexService;
import ru.tishembitov.pictorium.kafka.event.ContentEvent;
import ru.tishembitov.pictorium.repository.PinSearchRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PinIndexServiceImpl implements PinIndexService {

    private final PinSearchRepository pinSearchRepository;

    @Override
    public void indexPin(ContentEvent event) {
        try {
            PinDocument document = mapToDocument(event);
            pinSearchRepository.save(document);
            log.info("Pin indexed: id={}, title={}", document.getId(), document.getTitle());
        } catch (Exception e) {
            log.error("Failed to index pin: {}", event.getPinId(), e);
            throw new IndexingException("Failed to index pin: " + event.getPinId(), e);
        }
    }

    @Override
    public void updatePin(ContentEvent event) {
        try {
            String pinId = event.getPinId().toString();

            Optional<PinDocument> existingOpt = pinSearchRepository.findById(pinId);

            if (existingOpt.isEmpty()) {
                log.warn("Pin not found for update, indexing as new: {}", pinId);
                indexPin(event);
                return;
            }

            PinDocument existing = existingOpt.get();
            updateDocumentFromEvent(existing, event);
            existing.setUpdatedAt(Instant.now());

            pinSearchRepository.save(existing);
            log.info("Pin updated: id={}", pinId);

        } catch (Exception e) {
            log.error("Failed to update pin: {}", event.getPinId(), e);
            throw new IndexingException("Failed to update pin: " + event.getPinId(), e);
        }
    }

    @Override
    public void updatePinCounters(ContentEvent event) {
        try {
            String pinId = event.getPinId().toString();

            Optional<PinDocument> existingOpt = pinSearchRepository.findById(pinId);

            if (existingOpt.isEmpty()) {
                log.warn("Pin not found for counter update: {}", pinId);
                return;
            }

            PinDocument existing = existingOpt.get();

            if (event.getLikeCount() != null) {
                existing.setLikeCount(event.getLikeCount());
            }
            if (event.getSaveCount() != null) {
                existing.setSaveCount(event.getSaveCount());
            }
            if (event.getCommentCount() != null) {
                existing.setCommentCount(event.getCommentCount());
            }
            if (event.getViewCount() != null) {
                existing.setViewCount(event.getViewCount());
            }

            existing.setUpdatedAt(Instant.now());
            pinSearchRepository.save(existing);

            log.debug("Pin counters updated: id={}", pinId);

        } catch (Exception e) {
            log.error("Failed to update pin counters: {}", event.getPinId(), e);
        }
    }

    @Override
    public void deletePin(String pinId) {
        try {
            if (pinSearchRepository.existsById(pinId)) {
                pinSearchRepository.deleteById(pinId);
                log.info("Pin deleted from index: {}", pinId);
            } else {
                log.warn("Pin not found for deletion: {}", pinId);
            }
        } catch (Exception e) {
            log.error("Failed to delete pin: {}", pinId, e);
            throw new IndexingException("Failed to delete pin: " + pinId, e);
        }
    }

    @Override
    public Optional<PinDocument> findById(String pinId) {
        return pinSearchRepository.findById(pinId);
    }

    @Override
    public void reindexAll(List<ContentEvent> pins) {
        log.info("Starting bulk reindex of {} pins", pins.size());

        List<PinDocument> documents = pins.stream()
                .map(this::mapToDocument)
                .toList();

        pinSearchRepository.saveAll(documents);

        log.info("Bulk reindex completed: {} pins", documents.size());
    }

    @Override
    public long count() {
        return pinSearchRepository.count();
    }

    private PinDocument mapToDocument(ContentEvent event) {
        return PinDocument.builder()
                .id(event.getPinId().toString())
                .title(event.getPinTitle())
                .description(event.getPinDescription())
                .tags(event.getPinTags() != null ? event.getPinTags() : new HashSet<>())
                .authorId(event.getActorId())
                .authorUsername(event.getActorUsername())
                .imageId(event.getImageId())
                .thumbnailId(event.getThumbnailId())
                .originalWidth(event.getOriginalWidth())
                .originalHeight(event.getOriginalHeight())
                .likeCount(event.getLikeCount() != null ? event.getLikeCount() : 0)
                .saveCount(event.getSaveCount() != null ? event.getSaveCount() : 0)
                .commentCount(event.getCommentCount() != null ? event.getCommentCount() : 0)
                .viewCount(event.getViewCount() != null ? event.getViewCount() : 0)
                .createdAt(event.getTimestamp())
                .updatedAt(Instant.now())
                .build();
    }

    private void updateDocumentFromEvent(PinDocument document, ContentEvent event) {
        if (event.getPinTitle() != null) {
            document.setTitle(event.getPinTitle());
        }
        if (event.getPinDescription() != null) {
            document.setDescription(event.getPinDescription());
        }
        if (event.getPinTags() != null) {
            document.setTags(event.getPinTags());
        }
        if (event.getImageId() != null) {
            document.setImageId(event.getImageId());
        }
        if (event.getThumbnailId() != null) {
            document.setThumbnailId(event.getThumbnailId());
        }
    }
}