package ru.tishembitov.pictorium.savedPin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.like.LikeRepository;
import ru.tishembitov.pictorium.pin.Pin;
import ru.tishembitov.pictorium.pin.PinMapper;
import ru.tishembitov.pictorium.pin.PinRepository;
import ru.tishembitov.pictorium.pin.PinResponse;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SavedPinServiceImpl implements SavedPinService {

    private final SavedPinRepository savedPinRepository;
    private final PinRepository pinRepository;
    private final LikeRepository likeRepository;
    private final PinMapper pinMapper;

    @Override
    public PinResponse savePin(UUID pinId) {
        String userId = SecurityUtils.requireCurrentUserId();

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin with id " + pinId + " not found"));

        boolean isLiked = likeRepository.existsByUserIdAndPinId(userId, pinId);

        if (savedPinRepository.existsByUserIdAndPinId(userId, pinId)) {
            return pinMapper.toResponse(pin, isLiked, true);
        }

        SavedPin savedPin = SavedPin.builder()
                .userId(userId)
                .pin(pin)
                .build();

        savedPinRepository.save(savedPin);
        pinRepository.incrementSaveCount(pinId);

        return pinMapper.toResponse(pin, isLiked, true);
    }

    @Override
    public void unsavePin(UUID pinId) {
        String userId = SecurityUtils.requireCurrentUserId();

        if (!pinRepository.existsById(pinId)) {
            throw new ResourceNotFoundException("Pin with id " + pinId + " not found");
        }

        SavedPin savedPin = savedPinRepository.findByUserIdAndPinId(userId, pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin is not saved"));

        savedPinRepository.delete(savedPin);
        pinRepository.decrementSaveCount(pinId);
    }
}