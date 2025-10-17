package ru.tishembitov.pictorium.pin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.AccessDeniedException;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.like.LikeRepository;
import ru.tishembitov.pictorium.savedPins.SavedPinRepository;
import ru.tishembitov.pictorium.tag.Tag;
import ru.tishembitov.pictorium.tag.TagService;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PinServiceImpl implements PinService {

    private final PinRepository pinRepository;
    private final PinMapper pinMapper;
    private final TagService tagService;
    private final LikeRepository likeRepository;
    private final SavedPinRepository savedPinRepository;
    private final SecurityUtils securityUtils;

    @Override
    public PinResponse getPinById(UUID id) {
        Pin pin = pinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pin with id " + id + " not found"));

        PinUserInteraction interaction = getPinUserInteraction(id);

        return pinMapper.toResponse(pin, interaction.isLiked(), interaction.isSaved());
    }

    @Override
    public Page<PinResponse> findPins(PinFilter filter, Pageable pageable) {
        normalizeScope(filter);

        Specification<Pin> spec = PinSpecifications.build(filter);
        Page<Pin> pins = pinRepository.findAll(spec, pageable);

        if (pins.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<UUID> pinIds = pins.getContent().stream()
                .map(Pin::getId)
                .collect(Collectors.toSet());

        Map<UUID, PinUserInteraction> interactions = getPinUserInteractionsBatch(pinIds);

        return pins.map(pin -> {
            PinUserInteraction interaction = interactions.getOrDefault(
                    pin.getId(),
                    PinUserInteraction.empty()
            );
            return pinMapper.toResponse(pin, interaction.isLiked(), interaction.isSaved());
        });
    }
    @Override
    @Transactional
    public PinResponse createPin(PinCreateRequest request) {
        String currentUserId = securityUtils.requireCurrentUserId();
        Pin pin = pinMapper.toEntity(currentUserId, request);

        if (request.tags() != null && !request.tags().isEmpty()) {
            Set<Tag> tags = tagService.getOrCreateTags(request.tags());
            pin.setTags(tags);
        }

        pin = pinRepository.save(pin);

        return pinMapper.toResponse(pin, false, false);
    }

    @Override
    @Transactional
    public PinResponse updatePin(UUID id, PinUpdateRequest request) {
        Pin pin = pinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pin with id " + id + " not found"));

        String currentUserId = securityUtils.requireCurrentUserId();
        checkPinOwnership(pin, currentUserId);

        pinMapper.updateEntity(pin, request);
        pin = pinRepository.save(pin);

        PinUserInteraction interaction = getPinUserInteraction(id);

        return pinMapper.toResponse(pin, interaction.isLiked(), interaction.isSaved());
    }

    @Override
    @Transactional
    public void deletePin(UUID id) {
        Pin pin = pinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pin with id " + id + " not found"));
        String currentUserId = securityUtils.requireCurrentUserId();

        checkPinOwnership(pin, currentUserId);
        pinRepository.delete(pin);
    }

    private PinUserInteraction getPinUserInteraction(UUID pinId) {
        return securityUtils.getCurrentUserId()
                .map(userId -> calculateUserInteraction(userId, pinId))
                .orElseGet(PinUserInteraction::empty);
    }

    private PinUserInteraction calculateUserInteraction(String userId, UUID pinId) {
        boolean isLiked = likeRepository.existsByUserIdAndPinId(userId, pinId);
        boolean isSaved = savedPinRepository.existsByUserIdAndPinId(userId, pinId);
        return new PinUserInteraction(isLiked, isSaved);
    }

    private Map<UUID, PinUserInteraction> getPinUserInteractionsBatch(Set<UUID> pinIds) {
        return securityUtils.getCurrentUserId()
                .map(userId -> calculateUserInteractionsBatch(userId, pinIds))
                .orElseGet(() -> createEmptyInteractions(pinIds));
    }

    private Map<UUID, PinUserInteraction> calculateUserInteractionsBatch(String userId, Set<UUID> pinIds) {
        Set<UUID> likedPinIds = likeRepository.findPinIdsByUserIdAndPinIdIn(userId, pinIds);
        Set<UUID> savedPinIds = savedPinRepository.findPinIdsByUserIdAndPinIdIn(userId, pinIds);

        return pinIds.stream()
                .collect(Collectors.toMap(
                        pinId -> pinId,
                        pinId -> new PinUserInteraction(
                                likedPinIds.contains(pinId),
                                savedPinIds.contains(pinId)
                        )
                ));
    }

    private Map<UUID, PinUserInteraction> createEmptyInteractions(Set<UUID> pinIds) {
        return pinIds.stream()
                .collect(Collectors.toMap(
                        pinId -> pinId,
                        pinId -> PinUserInteraction.empty()
                ));
    }

    private void checkPinOwnership(Pin pin, String userId) {
        if (!pin.getAuthorId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to modify this pin");
        }
    }

    record PinUserInteraction(boolean isLiked, boolean isSaved) {
        static PinUserInteraction empty() {
            return new PinUserInteraction(false, false);
        }
    }

    private void normalizeScope(PinFilter filter) {
        if (filter == null || filter.scope() == null) {
            return;
        }

        switch (filter.scope()) {
            case CREATED -> filter
                    .withAuthorId(filter.authorId() != null ? filter.authorId() : securityUtils.requireCurrentUserId())
                    .withSavedBy(null)
                    .withLikedBy(null)
                    .withRelatedTo(null);

            case SAVED -> filter
                    .withAuthorId(null)
                    .withSavedBy(filter.savedBy() != null ? filter.savedBy() : securityUtils.requireCurrentUserId())
                    .withLikedBy(null)
                    .withRelatedTo(null);

            case LIKED -> filter
                    .withAuthorId(null)
                    .withSavedBy(null)
                    .withLikedBy(filter.likedBy() != null ? filter.likedBy() : securityUtils.requireCurrentUserId())
                    .withRelatedTo(null);

            case RELATED -> {
                if (filter.relatedTo() == null) {
                    throw new IllegalArgumentException("relatedTo parameter is required for RELATED scope");
                }
                filter
                        .withAuthorId(null)
                        .withSavedBy(null)
                        .withLikedBy(null);
            }

            case ALL -> {
            }
        }
    }
}
