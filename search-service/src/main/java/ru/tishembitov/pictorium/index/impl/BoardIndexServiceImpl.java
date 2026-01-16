package ru.tishembitov.pictorium.index.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.document.BoardDocument;
import ru.tishembitov.pictorium.exception.IndexingException;
import ru.tishembitov.pictorium.index.BoardIndexService;
import ru.tishembitov.pictorium.kafka.event.BoardEvent;
import ru.tishembitov.pictorium.repository.BoardSearchRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardIndexServiceImpl implements BoardIndexService {

    private final BoardSearchRepository boardSearchRepository;

    @Override
    public void indexBoard(BoardEvent event) {
        try {
            BoardDocument document = mapToDocument(event);
            boardSearchRepository.save(document);
            log.info("Board indexed: id={}, title={}", document.getId(), document.getTitle());
        } catch (Exception e) {
            log.error("Failed to index board: {}", event.getBoardId(), e);
            throw new IndexingException("Failed to index board: " + event.getBoardId(), e);
        }
    }

    @Override
    public void updateBoard(BoardEvent event) {
        try {
            String boardId = event.getBoardId().toString();

            Optional<BoardDocument> existingOpt = boardSearchRepository.findById(boardId);

            if (existingOpt.isEmpty()) {
                log.warn("Board not found for update, indexing as new: {}", boardId);
                indexBoard(event);
                return;
            }

            BoardDocument existing = existingOpt.get();

            if (event.getBoardTitle() != null) {
                existing.setTitle(event.getBoardTitle());
            }
            if (event.getPreviewImageId() != null) {
                existing.setPreviewImageId(event.getPreviewImageId());
            }

            existing.setUpdatedAt(Instant.now());
            boardSearchRepository.save(existing);

            log.info("Board updated: id={}", boardId);

        } catch (Exception e) {
            log.error("Failed to update board: {}", event.getBoardId(), e);
            throw new IndexingException("Failed to update board: " + event.getBoardId(), e);
        }
    }

    @Override
    public void updateBoardPinCount(BoardEvent event) {
        try {
            String boardId = event.getBoardId().toString();

            Optional<BoardDocument> existingOpt = boardSearchRepository.findById(boardId);

            if (existingOpt.isEmpty()) {
                log.warn("Board not found for pin count update: {}", boardId);
                return;
            }

            BoardDocument existing = existingOpt.get();

            if (event.getPinCount() != null) {
                existing.setPinCount(event.getPinCount());
            }
            if (event.getPreviewImageId() != null) {
                existing.setPreviewImageId(event.getPreviewImageId());
            }

            existing.setUpdatedAt(Instant.now());
            boardSearchRepository.save(existing);

            log.debug("Board pin count updated: id={}, count={}", boardId, event.getPinCount());

        } catch (Exception e) {
            log.error("Failed to update board pin count: {}", event.getBoardId(), e);
        }
    }

    @Override
    public void deleteBoard(String boardId) {
        try {
            if (boardSearchRepository.existsById(boardId)) {
                boardSearchRepository.deleteById(boardId);
                log.info("Board deleted from index: {}", boardId);
            } else {
                log.warn("Board not found for deletion: {}", boardId);
            }
        } catch (Exception e) {
            log.error("Failed to delete board: {}", boardId, e);
            throw new IndexingException("Failed to delete board: " + boardId, e);
        }
    }

    @Override
    public Optional<BoardDocument> findById(String boardId) {
        return boardSearchRepository.findById(boardId);
    }

    @Override
    public long count() {
        return boardSearchRepository.count();
    }

    private BoardDocument mapToDocument(BoardEvent event) {
        return BoardDocument.builder()
                .id(event.getBoardId().toString())
                .title(event.getBoardTitle())
                .userId(event.getActorId())
                .username(event.getActorUsername())
                .pinCount(event.getPinCount() != null ? event.getPinCount() : 0)
                .previewImageId(event.getPreviewImageId())
                .createdAt(event.getTimestamp())
                .updatedAt(Instant.now())
                .build();
    }
}