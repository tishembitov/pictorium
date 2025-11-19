package ru.tishembitov.pictorium.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageUrlService {

    private final StorageClient storageClient;

    @Cacheable(value = "imageUrls", key = "#imageId", unless = "#result == null")
    public String getImageUrl(String imageId) {
        if (imageId == null || imageId.isBlank()) {
            return null;
        }

        try {
            return storageClient.getImageUrl(imageId, null);
        } catch (Exception e) {
            log.error("Failed to get image URL for imageId: {}", imageId, e);
            return null;
        }
    }

    public void validateImageExists(String imageId) {
        if (imageId == null || imageId.isBlank()) {
            throw new IllegalArgumentException("Image ID cannot be empty");
        }

        try {
            storageClient.getImageMetadata(imageId);
        } catch (Exception e) {
            log.error("Image not found in storage-service: {}", imageId, e);
            throw new ResourceNotFoundException("Image not found: " + imageId);
        }
    }

    public void deleteImageSafely(String imageId) {
        if (imageId == null || imageId.isBlank()) {
            return;
        }

        try {
            storageClient.deleteImage(imageId);
            log.info("Deleted image from storage: {}", imageId);
        } catch (Exception e) {
            log.error("Failed to delete image from storage: {}", imageId, e);
        }
    }
}