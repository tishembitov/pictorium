package ru.tishembitov.pictorium.savedPin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.board.BoardRepository;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.pin.*;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedPinServiceImpl implements SavedPinService{

    private final PinRepository pinRepository;
    private final SavedPinRepository savedPinRepository;
    private final BoardRepository boardRepository;
    private final PinMapper pinMapper;
    private final PinService pinService;

    @Override
    @Transactional
    public PinResponse saveToProfile(UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        if (savedPinRepository.existsByUserIdAndPinId(currentUserId, pinId)) {
            log.debug("Pin {} already saved to profile by user {}", pinId, currentUserId);
            return pinMapper.toResponse(pin, pinService.getPinInteractionDto(pinId));
        }

        boolean wasAlreadySaved = isPinSavedByUser(currentUserId, pinId);

        SavedPin savedPin = SavedPin.builder()
                .userId(currentUserId)
                .pin(pin)
                .build();
        savedPinRepository.save(savedPin);

        if (!wasAlreadySaved) {
            pinRepository.incrementSaveCount(pinId);
        }

        log.info("Pin saved to profile: pinId={}, userId={}", pinId, currentUserId);

        return pinMapper.toResponse(pin, pinService.getPinInteractionDto(pinId));
    }

    @Override
    @Transactional
    public void unsaveFromProfile(UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        if (!pinRepository.existsById(pinId)) {
            throw new ResourceNotFoundException("Pin not found with id: " + pinId);
        }

        if (!savedPinRepository.existsByUserIdAndPinId(currentUserId, pinId)) {
            throw new ResourceNotFoundException("Pin is not saved to your profile");
        }

        savedPinRepository.deleteByUserIdAndPinId(currentUserId, pinId);

        boolean stillSavedInBoards = boardRepository.isPinSavedByUser(currentUserId, pinId);

        if (!stillSavedInBoards) {
            pinRepository.decrementSaveCount(pinId);
        }

        log.info("Pin unsaved from profile: pinId={}, userId={}", pinId, currentUserId);
    }

    @Override
    public Page<PinResponse> getSavedToProfilePins(String userId, Pageable pageable) {
        Page<Pin> pins = savedPinRepository.findSavedPinsByUserId(userId, pageable);

        if (pins.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<UUID> pinIds = pins.getContent().stream()
                .map(Pin::getId)
                .collect(Collectors.toSet());

        Map<UUID, PinInteractionDto> interactions = pinService.getPinInteractionDtosBatch(pinIds);

        return pins.map(pin -> {
            PinInteractionDto interaction = interactions.getOrDefault(
                    pin.getId(),
                    PinInteractionDto.empty()
            );
            return pinMapper.toResponse(pin, interaction);
        });
    }

    @Override
    public boolean isPinSavedToProfile(UUID pinId) {
        return SecurityUtils.getCurrentUserId()
                .map(userId -> savedPinRepository.existsByUserIdAndPinId(userId, pinId))
                .orElse(false);
    }

    private boolean isPinSavedByUser(String userId, UUID pinId) {
        if (savedPinRepository.existsByUserIdAndPinId(userId, pinId)) {
            return true;
        }
        return boardRepository.isPinSavedByUser(userId, pinId);
    }
}
