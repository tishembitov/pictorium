package ru.tishembitov.pictorium.like;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.pin.Pin;
import ru.tishembitov.pictorium.pin.PinMapper;
import ru.tishembitov.pictorium.pin.PinRepository;
import ru.tishembitov.pictorium.pin.PinResponse;
import ru.tishembitov.pictorium.savedPin.SavedPinRepository;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final PinRepository pinRepository;
    private final SavedPinRepository savedPinRepository;
    private final PinMapper pinMapper;

    @Override
    public PinResponse likePin(UUID pinId) {
        String userId = SecurityUtils.requireCurrentUserId();

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin with id " + pinId + " not found"));

        boolean isSaved = savedPinRepository.existsByUserIdAndPinId(userId, pinId);

        if (likeRepository.existsByUserIdAndPinId(userId, pinId)) {
            return pinMapper.toResponse(pin, true, isSaved);
        }

        Like like = Like.builder()
                .userId(userId)
                .pin(pin)
                .build();

        likeRepository.save(like);
        pinRepository.incrementLikeCount(pinId);

        return pinMapper.toResponse(pin, true, isSaved);
    }

    @Override
    public void unlikePin(UUID pinId) {
        String userId = SecurityUtils.requireCurrentUserId();

        if (!pinRepository.existsById(pinId)) {
            throw new ResourceNotFoundException("Pin with id " + pinId + " not found");
        }

        Like like = likeRepository.findByUserIdAndPinId((userId), pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin is not liked"));

        likeRepository.delete(like);
        pinRepository.decrementLikeCount(pinId);
    }
}
