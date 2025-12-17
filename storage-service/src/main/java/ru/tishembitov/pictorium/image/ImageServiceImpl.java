// ================================================
// FILE: ImageServiceImpl.java
// ================================================
package ru.tishembitov.pictorium.image;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.config.MinioProperties;
import ru.tishembitov.pictorium.exception.ImageNotFoundException;
import ru.tishembitov.pictorium.exception.ImageStorageException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final ThumbnailService thumbnailService;
    private final ImageUrlService imageUrlService;

    @Value("${image-storage.max-file-size:10485760}")
    private long maxFileSize;

    @Value("${image-storage.allowed-content-types:image/jpeg,image/jpg,image/png,image/gif,image/webp}")
    private Set<String> allowedContentTypes;

    @Value("${image-storage.presigned-url-expiry-minutes:15}")
    private int presignedUploadExpiryMinutes;

    @Value("${image-storage.download-url-expiry-hours:24}")
    private int downloadUrlExpiryHours;

    @Value("${image-storage.thumbnail.default-width:236}")
    private int defaultThumbnailWidth;

    @Value("${image-storage.thumbnail.default-height:350}")
    private int defaultThumbnailHeight;

    @Override
    @Transactional
    public PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request) {
        validateUploadRequest(request);

        String imageId = generateImageId();
        String extension = extractFileExtension(request.getFileName());
        String objectName = buildObjectName(imageId, request.getCategory(), extension);

        String uploadUrl = imageUrlService.generatePresignedPutUrl(
                minioProperties.getBucketName(), objectName
        );

        Image record = imageMapper.toEntity(request, imageId, objectName, minioProperties.getBucketName());

        if (request.isGenerateThumbnail() && hasThumbnailBucket()) {
            setupThumbnailGeneration(record, request);
        }

        imageRepository.save(record);

        long expiresAt = imageUrlService.calculateExpiryTimestamp(presignedUploadExpiryMinutes);
        Map<String, String> headers = Map.of("Content-Type", request.getContentType());

        log.info("Generated presigned upload URL. ImageId: {}, ThumbnailId: {}",
                imageId, record.getThumbnailImageId());

        return imageMapper.toPresignedResponse(
                imageId, uploadUrl, expiresAt, headers, record.getThumbnailImageId()
        );
    }

    @Override
    @Transactional
    public ConfirmUploadResponse confirmUpload(ConfirmUploadRequest request) {
        Image record = findImageById(request.getImageId());

        if (record.isConfirmed()) {
            log.warn("Image already confirmed: {}", request.getImageId());
            return buildConfirmResponse(record);
        }

        if (!record.isPending()) {
            throw new ImageStorageException("Cannot confirm image with status: " + record.getStatus());
        }

        try {
            StatObjectResponse stat = verifyObjectExistsInMinio(record);

            String fileName = request.getFileName() != null ? request.getFileName() : record.getFileName();
            imageMapper.updateOnConfirm(record, stat.size(), stat.contentType(), fileName);

            if (shouldGenerateThumbnail(record)) {
                generateAndSaveThumbnail(record);
            }

            imageRepository.save(record);
            log.info("Upload confirmed. ImageId: {}", request.getImageId());

            return buildConfirmResponse(record);

        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new ImageNotFoundException("Image file not found in storage. Upload may have failed.");
            }
            throw new ImageStorageException("Failed to verify upload", e);
        } catch (ImageNotFoundException | ImageStorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to confirm upload: {}", request.getImageId(), e);
            throw new ImageStorageException("Failed to confirm upload", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ImageUrlResponse getImageUrl(String imageId, Integer expirySeconds) {
        Image record = findConfirmedImage(imageId);

        int expiry = (expirySeconds != null && expirySeconds > 0)
                ? expirySeconds
                : downloadUrlExpiryHours * 3600;

        String url = imageUrlService.generatePresignedGetUrl(
                record.getBucketName(), record.getObjectName(), expiry
        );
        long expiresAt = imageUrlService.calculateExpiryTimestampSeconds(expiry);

        return imageMapper.toUrlResponse(imageId, url, expiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageMetadata getImageMetadata(String imageId) {
        Image record = findConfirmedImage(imageId);
        return imageMapper.toMetadata(record);
    }

    @Override
    @Transactional
    public void deleteImage(String imageId) {
        Image record = findImageById(imageId);

        try {
            deleteFromMinio(record.getBucketName(), record.getObjectName());

            if (record.getThumbnailImageId() != null) {
                deleteThumbnailSafely(record.getThumbnailImageId());
            }

            record.setStatus(Image.ImageStatus.DELETED);
            imageRepository.save(record);

            log.info("Image deleted: {}", imageId);

        } catch (Exception e) {
            log.error("Failed to delete image: {}", imageId, e);
            throw new ImageStorageException("Failed to delete image", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImageMetadata> listImagesByCategory(String category) {
        List<Image> records = (category != null)
                ? imageRepository.findByCategoryAndStatus(category, Image.ImageStatus.CONFIRMED)
                : imageRepository.findByStatus(Image.ImageStatus.CONFIRMED);

        return imageMapper.toMetadataList(records);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadImage(String imageId) {
        Image record = findConfirmedImage(imageId);

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(record.getBucketName())
                            .object(record.getObjectName())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download image: {}", imageId, e);
            throw new ImageStorageException("Failed to download image", e);
        }
    }

    private void validateUploadRequest(PresignedUploadRequest request) {
        if (request.getFileSize() > maxFileSize) {
            throw new ImageStorageException(String.format(
                    "File size %d bytes exceeds maximum %d bytes",
                    request.getFileSize(), maxFileSize
            ));
        }

        String contentType = request.getContentType().toLowerCase();
        if (!allowedContentTypes.contains(contentType)) {
            throw new ImageStorageException(String.format(
                    "Content type '%s' is not allowed. Allowed: %s",
                    contentType, allowedContentTypes
            ));
        }

        if (request.isGenerateThumbnail() && !thumbnailService.isFormatSupported(contentType)) {
            log.warn("Thumbnail generation requested but format '{}' may not be fully supported", contentType);
        }
    }

    private void setupThumbnailGeneration(Image record, PresignedUploadRequest request) {
        String thumbnailImageId = generateImageId();
        record.setThumbnailImageId(thumbnailImageId);

        record.setThumbnailWidth(
                request.getThumbnailWidth() != null ? request.getThumbnailWidth() : defaultThumbnailWidth
        );
        record.setThumbnailHeight(
                request.getThumbnailHeight() != null ? request.getThumbnailHeight() : defaultThumbnailHeight
        );

        log.info("Thumbnail configured. Id: {}, dimensions: {}x{}",
                thumbnailImageId, record.getThumbnailWidth(), record.getThumbnailHeight());
    }

    private boolean shouldGenerateThumbnail(Image record) {
        return record.getThumbnailImageId() != null && hasThumbnailBucket();
    }

    private void generateAndSaveThumbnail(Image record) {
        log.info("Generating thumbnail for image: {}", record.getId());

        try (InputStream originalStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(record.getBucketName())
                        .object(record.getObjectName())
                        .build()
        )) {
            byte[] thumbnailBytes = thumbnailService.generateThumbnail(
                    originalStream,
                    record.getThumbnailWidth(),
                    record.getThumbnailHeight()
            );

            String thumbnailObjectName = buildObjectName(
                    record.getThumbnailImageId(),
                    record.getCategory(),
                    "jpg"
            );

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getThumbnailBucket())
                            .object(thumbnailObjectName)
                            .stream(new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length, -1)
                            .contentType("image/jpeg")
                            .build()
            );

            Image thumbnailRecord = Image.builder()
                    .id(record.getThumbnailImageId())
                    .objectName(thumbnailObjectName)
                    .bucketName(minioProperties.getThumbnailBucket())
                    .fileName("thumb_" + record.getFileName())
                    .contentType("image/jpeg")
                    .fileSize((long) thumbnailBytes.length)
                    .category(record.getCategory())
                    .status(Image.ImageStatus.CONFIRMED)
                    .confirmedAt(Instant.now())
                    .build();

            imageRepository.save(thumbnailRecord);

            log.info("Thumbnail generated successfully. Id: {}, size: {} bytes",
                    record.getThumbnailImageId(), thumbnailBytes.length);

        } catch (Exception e) {
            log.error("Failed to generate thumbnail for image: {}", record.getId(), e);
            record.setThumbnailImageId(null);
        }
    }

    private ConfirmUploadResponse buildConfirmResponse(Image record) {
        String imageUrl = imageUrlService.generatePresignedGetUrlSafely(
                record.getBucketName(),
                record.getObjectName(),
                null
        );

        String thumbnailUrl = null;
        if (record.getThumbnailImageId() != null) {
            // Нужно получить данные миниатюры из БД
            thumbnailUrl = imageRepository.findById(record.getThumbnailImageId())
                    .map(thumb -> imageUrlService.generatePresignedGetUrlSafely(
                            thumb.getBucketName(),
                            thumb.getObjectName(),
                            null
                    ))
                    .orElse(null);
        }

        return imageMapper.toConfirmResponse(record, imageUrl, thumbnailUrl);
    }

    private void deleteThumbnailSafely(String thumbnailImageId) {
        imageRepository.findById(thumbnailImageId).ifPresent(thumbnail -> {
            try {
                deleteFromMinio(thumbnail.getBucketName(), thumbnail.getObjectName());
                thumbnail.setStatus(Image.ImageStatus.DELETED);
                imageRepository.save(thumbnail);
            } catch (Exception e) {
                log.warn("Failed to delete thumbnail: {}", thumbnailImageId, e);
            }
        });
    }

    private String generateImageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String buildObjectName(String imageId, String category, String extension) {
        StringBuilder sb = new StringBuilder();

        if (category != null && !category.isBlank()) {
            sb.append(category.toLowerCase()).append("/");
        }

        sb.append(LocalDate.now()).append("/");
        sb.append(imageId).append(".").append(extension);

        return sb.toString();
    }

    private boolean hasThumbnailBucket() {
        return minioProperties.getThumbnailBucket() != null
                && !minioProperties.getThumbnailBucket().isBlank();
    }

    private Image findImageById(String imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException("Image not found: " + imageId));
    }

    private Image findConfirmedImage(String imageId) {
        return imageRepository.findByIdAndStatus(imageId, Image.ImageStatus.CONFIRMED)
                .orElseThrow(() -> new ImageNotFoundException("Confirmed image not found: " + imageId));
    }

    private StatObjectResponse verifyObjectExistsInMinio(Image record) throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(record.getBucketName())
                        .object(record.getObjectName())
                        .build()
        );
    }

    private void deleteFromMinio(String bucket, String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }
}