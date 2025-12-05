package ru.tishembitov.pictorium.image;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "image-storage.cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ImageCleanupScheduler {

    private final ImageRepository imageRepository;
    private final MinioClient minioClient;

    @Value("${image-storage.cleanup.pending-expiry-hours:2}")
    private int pendingExpiryHours;

    @Scheduled(fixedDelayString = "${image-storage.cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredPendingUploads() {
        Instant threshold = Instant.now().minus(pendingExpiryHours, ChronoUnit.HOURS);

        List<Image> expiredRecords = imageRepository
                .findExpiredPendingUploads(Image.ImageStatus.PENDING, threshold);

        log.info("Found {} expired pending uploads to cleanup", expiredRecords.size());

        expiredRecords.forEach(this::processExpiredRecord);
    }

    private void processExpiredRecord(Image record) {
        try {
            tryRemoveFromMinio(record);
            markAsExpired(record);
            log.debug("Marked as expired: {}", record.getId());
        } catch (Exception e) {
            log.error("Failed to cleanup expired record: {}", record.getId(), e);
        }
    }

    private void tryRemoveFromMinio(Image record) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(record.getBucketName())
                            .object(record.getObjectName())
                            .build()
            );
        } catch (Exception e) {
            log.debug("Object not found in MinIO, skipping: {}", record.getObjectName());
        }
    }

    private void markAsExpired(Image record) {
        record.setStatus(Image.ImageStatus.EXPIRED);
        imageRepository.save(record);
    }
}