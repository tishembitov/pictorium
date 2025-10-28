package ru.tishembitov.pictorium.pin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.tag.Tag;
import ru.tishembitov.pictorium.tag.TagService;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PinServiceImpl implements PinService {

    private final PinRepository pinRepository;
    private final PinMapper pinMapper;
    private final TagService tagService;

    @Override
    public PinResponse getPinById(UUID id) {
        Pin pin = pinRepository.findByIdWithTags(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pin with id " + id + " not found"));

        PinInteractionDto interaction = getPinInteractionDto(id);

        return pinMapper.toResponse(pin, interaction.isLiked(), interaction.isSaved());
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
            PinInteractionDto interaction = interactions.getOrDefault(
                    id,
                    PinInteractionDto.empty()
            );
            return pinMapper.toResponse(pin, interaction.isLiked(), interaction.isSaved());
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

        pin = pinRepository.save(pin);
        return pinMapper.toResponse(pin, false, false);
    }

    @Override
    @Transactional
    public PinResponse updatePin(UUID id, PinUpdateRequest request) {
        Pin pin = pinRepository.findByIdWithTags(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pin with id " + id + " not found"));

        String currentUserId = SecurityUtils.requireCurrentUserId();
        checkPinOwnership(pin, currentUserId);

        pinMapper.updateEntity(pin, request);

        if (request.tags() != null) {
            pin.getTags().clear();
            if (!request.tags().isEmpty()) {
                Set<Tag> tags = tagService.resolveTagsByNames(request.tags());
                pin.setTags(tags);
            }
        }

        pin = pinRepository.save(pin);

        PinInteractionDto interaction = getPinInteractionDto(id);

        return pinMapper.toResponse(pin, interaction.isLiked(), interaction.isSaved());
    }

    @Override
    @Transactional
    public void deletePin(UUID id) {
        Pin pin = pinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pin with id " + id + " not found"));

        String currentUserId = SecurityUtils.requireCurrentUserId();
        checkPinOwnership(pin, currentUserId);

        pinRepository.delete(pin);
    }


    private PinInteractionDto getPinInteractionDto(UUID pinId) {
        return SecurityUtils.getCurrentUserId()
                .map(userId -> {
                    Map<UUID, PinInteractionDto> interactions =
                            calculateUserInteractionsBatch(userId, Set.of(pinId));
                    return interactions.getOrDefault(pinId, PinInteractionDto.empty());
                })
                .orElseGet(PinInteractionDto::empty);
    }

    public Map<UUID, PinInteractionDto> getPinInteractionDtosBatch(Set<UUID> pinIds) {
        return SecurityUtils.getCurrentUserId()
                .map(userId -> calculateUserInteractionsBatch(userId, pinIds))
                .orElseGet(() -> createEmptyInteractions(pinIds));
    }

    private Map<UUID, PinInteractionDto> calculateUserInteractionsBatch(String userId, Set<UUID> pinIds) {
        if (pinIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<PinInteractionProjection> projections =
                pinRepository.findUserInteractions(userId, pinIds);

        Map<UUID, PinInteractionDto> result = new HashMap<>();
        pinIds.forEach(id -> result.put(id, PinInteractionDto.empty()));

        projections.forEach(proj ->
                result.put(proj.getId(), new PinInteractionDto(proj.getLiked(), proj.getSaved()))
        );

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

    private PinFilter normalizeScope(PinFilter filter) {
        if (filter == null || filter.scope() == null || filter.scope() == Scope.ALL) {
            return filter;
        }

        return switch (filter.scope()) {
            case CREATED -> filter
                    .withAuthorId(filter.authorId() != null
                            ? filter.authorId()
                            : SecurityUtils.requireCurrentUserId())
                    .withSavedBy(null)
                    .withLikedBy(null)
                    .withRelatedTo(null);

            case SAVED -> filter
                    .withAuthorId(null)
                    .withSavedBy(filter.savedBy() != null
                            ? filter.savedBy()
                            : SecurityUtils.requireCurrentUserId())
                    .withLikedBy(null)
                    .withRelatedTo(null);

            case LIKED -> filter
                    .withAuthorId(null)
                    .withSavedBy(null)
                    .withLikedBy(filter.likedBy() != null
                            ? filter.likedBy()
                            : SecurityUtils.requireCurrentUserId())
                    .withRelatedTo(null);

            case RELATED -> {
                if (filter.relatedTo() == null) {
                    throw new IllegalArgumentException(
                            "relatedTo parameter is required for RELATED scope"
                    );
                }
                yield filter
                        .withAuthorId(null)
                        .withSavedBy(null)
                        .withLikedBy(null);
            }
            default -> filter;
        };
    }
}