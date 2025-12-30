package ru.tishembitov.pictorium.pin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.board.BoardRepository;
import ru.tishembitov.pictorium.board.PinSaveInfoProjection;
import ru.tishembitov.pictorium.client.ImageService;
import ru.tishembitov.pictorium.comment.CommentRepository;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.like.LikeRepository;
import ru.tishembitov.pictorium.tag.Tag;
import ru.tishembitov.pictorium.tag.TagService;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PinServiceImpl implements PinService {

    private final PinRepository pinRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final PinMapper pinMapper;
    private final TagService tagService;
    private final ImageService imageService;

    @Override
    public PinResponse getPinById(UUID pinId) {
        Pin pin = pinRepository.findByIdWithTags(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin with id " + pinId + " not found"));

        PinInteractionDto interaction = getPinInteractionDto(pinId);

        return pinMapper.toResponse(pin, interaction);
    }

    @Override
    public Page<PinResponse> findPins(PinFilter filter, Pageable pageable) {
        filter = normalizeScope(filter);

        Specification<Pin> spec = PinSpecifications.withFilter(filter);

        Page<UUID> pinIds = pinRepository.findAll(spec, pageable)
                .map(Pin::getId);

        if (pinIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Pin> pins = pinRepository.findAllByIdWithTags(pinIds.getContent());

        Map<UUID, PinInteractionDto> interactions =
                getPinInteractionDtosBatch(new HashSet<>(pinIds.getContent()));

        Map<UUID, Pin> pinMap = pins.stream()
                .collect(Collectors.toMap(Pin::getId, pin -> pin));

        return pinIds.map(id -> {
            Pin pin = pinMap.get(id);
            PinInteractionDto interaction = interactions.getOrDefault(id, PinInteractionDto.empty());
            return pinMapper.toResponse(pin, interaction);
        });
    }

    @Override
    @Transactional
    public PinResponse createPin(PinCreateRequest request) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Pin pin = pinMapper.toEntity(currentUserId, request);

        if (request.tags() != null && !request.tags().isEmpty()) {
            Set<Tag> tags = tagService.resolveTagsByNames(request.tags());
            pin.setTags(tags);
        }

        Pin saved = pinRepository.save(pin);

        log.info("Pin created: {} by user: {}", saved.getId(), currentUserId);

        return pinMapper.toResponse(saved, PinInteractionDto.empty());
    }

    @Override
    @Transactional
    public PinResponse updatePin(UUID pinId, PinUpdateRequest request) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Pin pin = pinRepository.findByIdWithTags(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        checkPinOwnership(pin, currentUserId);

        handleImageUpdate(request.imageId(), pin.getImageId(), pin::setImageId);
        handleImageUpdate(request.thumbnailId(), pin.getThumbnailId(), pin::setThumbnailId);
        handleImageUpdate(request.videoPreviewId(), pin.getVideoPreviewId(), pin::setVideoPreviewId);

        pinMapper.updateEntity(pin, request);

        if (request.tags() != null) {
            pin.getTags().clear();
            if (!request.tags().isEmpty()) {
                Set<Tag> tags = tagService.resolveTagsByNames(request.tags());
                pin.setTags(tags);
            }
        }

        Pin updated = pinRepository.save(pin);

        PinInteractionDto interaction = getPinInteractionDto(pinId);

        log.info("Pin updated: {}", pinId);

        return pinMapper.toResponse(updated, interaction);
    }

    @Override
    @Transactional
    public void deletePin(UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        checkPinOwnership(pin, currentUserId);

        List<String> commentImageIds = commentRepository.findImageIdsByPinId(pinId);

        imageService.deleteImageSafely(pin.getImageId());
        imageService.deleteImageSafely(pin.getThumbnailId());
        imageService.deleteImageSafely(pin.getVideoPreviewId());

        commentImageIds.forEach(imageService::deleteImageSafely);

        pinRepository.delete(pin);

        log.info("Pin deleted: {} by user: {}", pinId, currentUserId);
    }

    @Override
    public Map<UUID, PinInteractionDto> getPinInteractionDtosBatch(Set<UUID> pinIds) {
        return SecurityUtils.getCurrentUserId()
                .map(userId -> calculateUserInteractionsBatch(userId, pinIds))
                .orElseGet(() -> createEmptyInteractions(pinIds));
    }

    @Override
    public PinInteractionDto getPinInteractionDto(UUID pinId) {
        return SecurityUtils.getCurrentUserId()
                .map(userId -> {
                    Map<UUID, PinInteractionDto> interactions =
                            calculateUserInteractionsBatch(userId, Set.of(pinId));
                    return interactions.getOrDefault(pinId, PinInteractionDto.empty());
                })
                .orElseGet(PinInteractionDto::empty);
    }

    private Map<UUID, PinInteractionDto> calculateUserInteractionsBatch(String userId, Set<UUID> pinIds) {
        if (pinIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<UUID> likedPinIds = likeRepository.findLikedPinIds(userId, pinIds);

        List<PinSaveInfoProjection> boardSaveInfos = boardRepository.findPinSaveInfo(userId, pinIds);
        Map<UUID, PinSaveInfoProjection> boardSaveMap = boardSaveInfos.stream()
                .collect(Collectors.toMap(PinSaveInfoProjection::getPinId, p -> p));

        Map<UUID, PinInteractionDto> result = new HashMap<>();

        for (UUID pinId : pinIds) {
            boolean isLiked = likedPinIds.contains(pinId);
            PinSaveInfoProjection boardInfo = boardSaveMap.get(pinId);

            if (boardInfo != null) {
                result.put(pinId, PinInteractionDto.saved(
                        isLiked,
                        boardInfo.getLastBoardName(),
                        boardInfo.getBoardCount().intValue()
                ));
            } else {
                result.put(pinId, PinInteractionDto.notSaved(isLiked));
            }
        }

        return result;
    }

    private Map<UUID, PinInteractionDto> createEmptyInteractions(Set<UUID> pinIds) {
        return pinIds.stream()
                .collect(Collectors.toMap(
                        pinId -> pinId,
                        pinId -> PinInteractionDto.empty()
                ));
    }

    private void checkPinOwnership(Pin pin, String userId) {
        if (!pin.getAuthorId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to modify this pin");
        }
    }

    private void handleImageUpdate(String newId, String currentId, Consumer<String> setter) {
        if (newId == null) return;

        if (newId.isBlank()) {
            imageService.deleteImageSafely(currentId);
            setter.accept(null);
            return;
        }

        if (!newId.equals(currentId)) {
            imageService.deleteImageSafely(currentId);
            setter.accept(newId);
        }
    }

    private PinFilter normalizeScope(PinFilter filter) {
        if (filter == null) {
            return PinFilter.empty();
        }

        if (filter.scope() == null || filter.scope() == Scope.ALL) {
            return filter;
        }

        return switch (filter.scope()) {
            case CREATED -> {
                String authorId = filter.authorId() != null
                        ? filter.authorId()
                        : SecurityUtils.requireCurrentUserId();
                yield filter.withAuthorId(authorId);
            }

            case SAVED -> {
                String savedBy = filter.savedBy() != null
                        ? filter.savedBy()
                        : SecurityUtils.requireCurrentUserId();
                yield filter.withSavedBy(savedBy);
            }

            case LIKED -> {
                String likedBy = filter.likedBy() != null
                        ? filter.likedBy()
                        : SecurityUtils.requireCurrentUserId();
                yield filter.withLikedBy(likedBy);
            }

            case RELATED -> {
                if (filter.relatedTo() == null) {
                    throw new IllegalArgumentException(
                            "relatedTo parameter is required for RELATED scope");
                }
                yield filter;
            }

            default -> filter;
        };
    }
}