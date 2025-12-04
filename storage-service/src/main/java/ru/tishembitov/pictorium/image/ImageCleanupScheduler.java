package ru.tishembitov.pictorium.image;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "image-storage.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class ImageCleanupScheduler {

    private final ImageRepository imageRepository;
    private final MinioClient minioClient;

    @Value("${image-storage.cleanup.pending-expiry-hours:2}")
    private int pendingExpiryHours;

    @Scheduled(fixedDelayString = "${image-storage.cleanup.interval-ms:3600000}") // Default: 1 hour
    @Transactional
    public void cleanupExpiredPendingUploads() {
        Instant threshold = Instant.now().minus(pendingExpiryHours, ChronoUnit.HOURS);

        List<Image> expiredRecords = imageRepository
                .findExpiredPendingUploads(Image.ImageStatus.PENDING, threshold);

        log.info("Found {} expired pending uploads to cleanup", expiredRecords.size());

        for (Image record : expiredRecords) {
            try {
                // Пытаемся удалить из MinIO (на случай если частично загружено)
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(record.getBucketName())
                                    .object(record.getObjectName())
                                    .build()
                    );
                } catch (Exception e) {
                    // Игнорируем - файла может не быть
                }

                record.setStatus(Image.ImageStatus.EXPIRED);
                imageRepository.save(record);

                log.debug("Marked as expired: {}", record.getId());

            } catch (Exception e) {
                log.error("Failed to cleanup expired record: {}", record.getId(), e);
            }
        }
    }
}