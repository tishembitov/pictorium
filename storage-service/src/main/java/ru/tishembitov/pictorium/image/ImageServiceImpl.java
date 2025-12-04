package ru.tishembitov.pictorium.image;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.config.MinioConfig;
import ru.tishembitov.pictorium.exception.ImageNotFoundException;
import ru.tishembitov.pictorium.exception.ImageStorageException;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final ImageRepository ImageRepository;

    @Value("${image-storage.max-file-size:10485760}")
    private Long maxFileSize;

    @Value("${image-storage.allowed-content-types:image/jpeg,image/jpg,image/png,image/gif,image/webp}")
    private Set<String> allowedContentTypes;

    @Value("${image-storage.presigned-url-expiry-minutes:15}")
    private Integer presignedUploadExpiryMinutes;

    @Value("${image-storage.download-url-expiry-hours:24}")
    private Integer downloadUrlExpiryHours;

    @Override
    @Transactional
    public PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request) {
        validateUploadRequest(request);

        String imageId = generateImageId();
        String extension = getFileExtension(request.getFileName());
        String objectName = buildObjectName(imageId, request.getCategory(), extension);

        try {
            // Генерируем presigned URL для PUT
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .expiry(presignedUploadExpiryMinutes, TimeUnit.MINUTES)
                            .build()
            );

            // Сохраняем запись о pending загрузке
            Image record = Image.builder()
                    .id(imageId)
                    .objectName(objectName)
                    .bucketName(minioConfig.getBucketName())
                    .fileName(request.getFileName())
                    .contentType(request.getContentType())
                    .fileSize(request.getFileSize())
                    .category(request.getCategory())
                    .status(Image.ImageStatus.PENDING)
                    .build();

            // Обработка thumbnail
            String thumbnailImageId = null;
            String thumbnailUploadUrl = null;
            String thumbnailObjectName = null;

            if (request.isGenerateThumbnail() && minioConfig.getThumbnailBucket() != null) {
                thumbnailImageId = generateImageId();
                thumbnailObjectName = buildObjectName(thumbnailImageId, request.getCategory(), "jpg");

                thumbnailUploadUrl = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(minioConfig.getThumbnailBucket())
                                .object(thumbnailObjectName)
                                .expiry(presignedUploadExpiryMinutes, TimeUnit.MINUTES)
                                .build()
                );

                record.setThumbnailImageId(thumbnailImageId);

                // Сохраняем запись для thumbnail
                Image thumbnailRecord = Image.builder()
                        .id(thumbnailImageId)
                        .objectName(thumbnailObjectName)
                        .bucketName(minioConfig.getThumbnailBucket())
                        .fileName("thumb_" + request.getFileName())
                        .contentType("image/jpeg")
                        .category(request.getCategory())
                        .status(Image.ImageStatus.PENDING)
                        .build();

                ImageRepository.save(thumbnailRecord);
            }

            ImageRepository.save(record);

            long expiresAt = System.currentTimeMillis() +
                    (presignedUploadExpiryMinutes * 60 * 1000L);

            Map<String, String> requiredHeaders = new HashMap<>();
            requiredHeaders.put("Content-Type", request.getContentType());

            log.info("Generated presigned upload URL for imageId: {}", imageId);

            return PresignedUploadResponse.builder()
                    .imageId(imageId)
                    .uploadUrl(uploadUrl)
                    .expiresAt(expiresAt)
                    .requiredHeaders(requiredHeaders)
                    .thumbnailImageId(thumbnailImageId)
                    .thumbnailUploadUrl(thumbnailUploadUrl)
                    .thumbnailObjectName(thumbnailObjectName)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            throw new ImageStorageException("Failed to generate upload URL: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ConfirmUploadResponse confirmUpload(ConfirmUploadRequest request) {
        Image record = ImageRepository.findById(request.getImageId())
                .orElseThrow(() -> new ImageNotFoundException("Image record not found: " + request.getImageId()));

        if (record.getStatus() == Image.ImageStatus.CONFIRMED) {
            log.warn("Image already confirmed: {}", request.getImageId());
            return buildConfirmResponse(record, true);
        }

        if (record.getStatus() != Image.ImageStatus.PENDING) {
            throw new ImageStorageException("Invalid image status for confirmation: " + record.getStatus());
        }

        try {
            // Проверяем, что файл действительно загружен в MinIO
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(record.getBucketName())
                            .object(record.getObjectName())
                            .build()
            );

            // Обновляем запись
            record.setStatus(Image.ImageStatus.CONFIRMED);
            record.setConfirmedAt(Instant.now());
            record.setFileSize(stat.size());
            record.setContentType(stat.contentType());

            if (request.getFileName() != null) {
                record.setFileName(request.getFileName());
            }

            // Подтверждаем thumbnail если есть
            if (record.getThumbnailImageId() != null) {
                confirmThumbnail(record.getThumbnailImageId());
            }

            ImageRepository.save(record);

            log.info("Upload confirmed for imageId: {}", request.getImageId());

            return buildConfirmResponse(record, true);

        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new ImageNotFoundException("Image file not found in storage. Upload may have failed.");
            }
            throw new ImageStorageException("Failed to verify upload: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to confirm upload for imageId: {}", request.getImageId(), e);
            throw new ImageStorageException("Failed to confirm upload: " + e.getMessage(), e);
        }
    }

    @Override
    public ImageUrlResponse getImageUrl(String imageId, Integer expirySeconds) {
        Image record = getConfirmedImage(imageId);

        try {
            int expiry = expirySeconds != null && expirySeconds > 0
                    ? expirySeconds
                    : downloadUrlExpiryHours * 3600;

            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(record.getBucketName())
                            .object(record.getObjectName())
                            .expiry(expiry, TimeUnit.SECONDS)
                            .build()
            );

            long expiresAt = System.currentTimeMillis() + (expiry * 1000L);

            return ImageUrlResponse.builder()
                    .imageId(imageId)
                    .url(url)
                    .expiresAt(expiresAt)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get image URL: {}", imageId, e);
            throw new ImageStorageException("Failed to get image URL: " + e.getMessage(), e);
        }
    }

    @Override
    public ImageMetadata getImageMetadata(String imageId) {
        Image record = getConfirmedImage(imageId);

        return ImageMetadata.builder()
                .imageId(record.getId())
                .fileName(record.getFileName())
                .contentType(record.getContentType())
                .size(record.getFileSize())
                .bucketName(record.getBucketName())
                .lastModified(record.getConfirmedAt() != null
                        ? record.getConfirmedAt().toEpochMilli()
                        : record.getCreatedAt().toEpochMilli())
                .build();
    }

    @Override
    @Transactional
    public void deleteImage(String imageId) {
        Image record = ImageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException("Image not found: " + imageId));

        try {
            // Удаляем из MinIO
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(record.getBucketName())
                            .object(record.getObjectName())
                            .build()
            );

            // Удаляем thumbnail если есть
            if (record.getThumbnailImageId() != null) {
                deleteThumbnail(record.getThumbnailImageId());
            }

            // Помечаем как удалённую
            record.setStatus(Image.ImageStatus.DELETED);
            ImageRepository.save(record);

            log.info("Image deleted: {}", imageId);

        } catch (Exception e) {
            log.error("Failed to delete image: {}", imageId, e);
            throw new ImageStorageException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ImageMetadata> listImagesByCategory(String category) {
        List<Image> records = category != null
                ? ImageRepository.findByCategoryAndStatus(category, Image.ImageStatus.CONFIRMED)
                : ImageRepository.findAll().stream()
                .filter(r -> r.getStatus() == Image.ImageStatus.CONFIRMED)
                .toList();

        return records.stream()
                .map(this::toMetadata)
                .collect(Collectors.toList());
    }

    @Override
    public InputStream downloadImage(String imageId) {
        Image record = getConfirmedImage(imageId);

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(record.getBucketName())
                            .object(record.getObjectName())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download image: {}", imageId, e);
            throw new ImageStorageException("Failed to download image: " + e.getMessage(), e);
        }
    }

    // ============ Private Methods ============

    private void validateUploadRequest(PresignedUploadRequest request) {
        if (request.getFileSize() > maxFileSize) {
            throw new ImageStorageException(
                    String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                            request.getFileSize(), maxFileSize)
            );
        }

        String contentType = request.getContentType().toLowerCase();
        if (!allowedContentTypes.contains(contentType)) {
            throw new ImageStorageException(
                    "Invalid content type: " + contentType + ". Allowed types: " + allowedContentTypes
            );
        }
    }

    private String generateImageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "jpg";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "jpg";
    }

    private String buildObjectName(String imageId, String category, String extension) {
        StringBuilder builder = new StringBuilder();

        if (category != null && !category.isEmpty()) {
            builder.append(category.toLowerCase()).append("/");
        }

        builder.append(LocalDate.now().toString()).append("/");
        builder.append(imageId).append(".").append(extension);

        return builder.toString();
    }

    private Image getConfirmedImage(String imageId) {
        return ImageRepository.findByIdAndStatus(imageId, Image.ImageStatus.CONFIRMED)
                .orElseThrow(() -> new ImageNotFoundException("Confirmed image not found: " + imageId));
    }

    private void confirmThumbnail(String thumbnailImageId) {
        ImageRepository.findById(thumbnailImageId)
                .ifPresent(thumbnail -> {
                    thumbnail.setStatus(Image.ImageStatus.CONFIRMED);
                    thumbnail.setConfirmedAt(Instant.now());
                    ImageRepository.save(thumbnail);
                });
    }

    private void deleteThumbnail(String thumbnailImageId) {
        ImageRepository.findById(thumbnailImageId)
                .ifPresent(thumbnail -> {
                    try {
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(thumbnail.getBucketName())
                                        .object(thumbnail.getObjectName())
                                        .build()
                        );
                        thumbnail.setStatus(Image.ImageStatus.DELETED);
                        ImageRepository.save(thumbnail);
                    } catch (Exception e) {
                        log.warn("Failed to delete thumbnail: {}", thumbnailImageId, e);
                    }
                });
    }

    private ConfirmUploadResponse buildConfirmResponse(Image record, boolean confirmed) {
        String imageUrl = null;
        String thumbnailUrl = null;

        try {
            imageUrl = getImageUrl(record.getId(), null).getUrl();
            if (record.getThumbnailImageId() != null) {
                thumbnailUrl = getImageUrl(record.getThumbnailImageId(), null).getUrl();
            }
        } catch (Exception e) {
            log.warn("Failed to generate URLs for confirm response", e);
        }

        return ConfirmUploadResponse.builder()
                .imageId(record.getId())
                .imageUrl(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .fileName(record.getFileName())
                .size(record.getFileSize())
                .contentType(record.getContentType())
                .uploadTimestamp(record.getConfirmedAt() != null
                        ? record.getConfirmedAt().toEpochMilli()
                        : System.currentTimeMillis())
                .confirmed(confirmed)
                .build();
    }

    private ImageMetadata toMetadata(Image record) {
        return ImageMetadata.builder()
                .imageId(record.getId())
                .fileName(record.getFileName())
                .contentType(record.getContentType())
                .size(record.getFileSize())
                .bucketName(record.getBucketName())
                .lastModified(record.getConfirmedAt() != null
                        ? record.getConfirmedAt().toEpochMilli()
                        : record.getCreatedAt().toEpochMilli())
                .build();
    }
}