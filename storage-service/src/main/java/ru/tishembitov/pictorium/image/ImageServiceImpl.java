// storage-service/src/main/java/ru/tishembitov/pictorium/image/ImageServiceImpl.java

package ru.tishembitov.pictorium.image;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.config.MinioConfig;
import ru.tishembitov.pictorium.exception.ImageNotFoundException;
import ru.tishembitov.pictorium.exception.ImageStorageException;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;

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
            String uploadUrl = generatePresignedPutUrl(minioConfig.getBucketName(), objectName);

            Image record = imageMapper.toImageEntity(
                    request,
                    imageId,
                    objectName,
                    minioConfig.getBucketName()
            );

            String thumbnailImageId = null;
            String thumbnailUploadUrl = null;
            String thumbnailObjectName = null;

            if (request.isGenerateThumbnail() && minioConfig.getThumbnailBucket() != null) {
                thumbnailImageId = generateImageId();
                thumbnailObjectName = buildObjectName(thumbnailImageId, request.getCategory(), "jpg");

                thumbnailUploadUrl = generatePresignedPutUrl(
                        minioConfig.getThumbnailBucket(),
                        thumbnailObjectName
                );

                record.setThumbnailImageId(thumbnailImageId);

                Image thumbnailRecord = imageMapper.toThumbnailEntity(
                        request,
                        thumbnailImageId,
                        thumbnailObjectName,
                        minioConfig.getThumbnailBucket()
                );

                imageRepository.save(thumbnailRecord);
            }

            imageRepository.save(record);

            long expiresAt = calculateExpiryTime(presignedUploadExpiryMinutes);
            Map<String, String> requiredHeaders = createRequiredHeaders(request.getContentType());

            log.info("Generated presigned upload URL for imageId: {}", imageId);

            return imageMapper.toPresignedUploadResponse(
                    imageId,
                    uploadUrl,
                    expiresAt,
                    requiredHeaders,
                    thumbnailImageId,
                    thumbnailUploadUrl,
                    thumbnailObjectName
            );

        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            throw new ImageStorageException("Failed to generate upload URL: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ConfirmUploadResponse confirmUpload(ConfirmUploadRequest request) {
        Image record = imageRepository.findById(request.getImageId())
                .orElseThrow(() -> new ImageNotFoundException(
                        "Image record not found: " + request.getImageId()));

        if (record.getStatus() == Image.ImageStatus.CONFIRMED) {
            log.warn("Image already confirmed: {}", request.getImageId());
            return buildConfirmResponse(record, true);
        }

        if (record.getStatus() != Image.ImageStatus.PENDING) {
            throw new ImageStorageException(
                    "Invalid image status for confirmation: " + record.getStatus());
        }

        try {
            StatObjectResponse stat = verifyObjectExists(record);

            imageMapper.updateOnConfirm(
                    record,
                    stat.size(),
                    stat.contentType(),
                    request.getFileName() != null ? request.getFileName() : record.getFileName()
            );

            if (record.getThumbnailImageId() != null) {
                confirmThumbnail(record.getThumbnailImageId());
            }

            imageRepository.save(record);

            log.info("Upload confirmed for imageId: {}", request.getImageId());

            return buildConfirmResponse(record, true);

        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new ImageNotFoundException(
                        "Image file not found in storage. Upload may have failed.");
            }
            throw new ImageStorageException("Failed to verify upload: " + e.getMessage(), e);
        } catch (ImageNotFoundException | ImageStorageException e) {
            throw e;
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

            String url = generatePresignedGetUrl(record.getBucketName(), record.getObjectName(), expiry);
            long expiresAt = System.currentTimeMillis() + (expiry * 1000L);

            return imageMapper.toImageUrlResponse(imageId, url, expiresAt);

        } catch (Exception e) {
            log.error("Failed to get image URL: {}", imageId, e);
            throw new ImageStorageException("Failed to get image URL: " + e.getMessage(), e);
        }
    }

    @Override
    public ImageMetadata getImageMetadata(String imageId) {
        Image record = getConfirmedImage(imageId);
        return imageMapper.toImageMetadata(record);
    }

    @Override
    @Transactional
    public void deleteImage(String imageId) {
        Image record = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException("Image not found: " + imageId));

        try {
            removeObjectFromMinio(record.getBucketName(), record.getObjectName());

            if (record.getThumbnailImageId() != null) {
                deleteThumbnail(record.getThumbnailImageId());
            }

            record.setStatus(Image.ImageStatus.DELETED);
            imageRepository.save(record);

            log.info("Image deleted: {}", imageId);

        } catch (Exception e) {
            log.error("Failed to delete image: {}", imageId, e);
            throw new ImageStorageException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ImageMetadata> listImagesByCategory(String category) {
        List<Image> records;

        if (category != null) {
            records = imageRepository.findByCategoryAndStatus(category, Image.ImageStatus.CONFIRMED);
        } else {
            records = imageRepository.findAll().stream()
                    .filter(r -> r.getStatus() == Image.ImageStatus.CONFIRMED)
                    .toList();
        }

        return imageMapper.toImageMetadataList(records);
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

        builder.append(LocalDate.now()).append("/");
        builder.append(imageId).append(".").append(extension);

        return builder.toString();
    }

    private String generatePresignedPutUrl(String bucket, String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucket)
                        .object(objectName)
                        .expiry(presignedUploadExpiryMinutes, TimeUnit.MINUTES)
                        .build()
        );
    }

    private String generatePresignedGetUrl(String bucket, String objectName, int expirySeconds)
            throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectName)
                        .expiry(expirySeconds, TimeUnit.SECONDS)
                        .build()
        );
    }

    private long calculateExpiryTime(int minutes) {
        return System.currentTimeMillis() + (minutes * 60 * 1000L);
    }

    private Map<String, String> createRequiredHeaders(String contentType) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", contentType);
        return headers;
    }

    private StatObjectResponse verifyObjectExists(Image record) throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(record.getBucketName())
                        .object(record.getObjectName())
                        .build()
        );
    }

    private void removeObjectFromMinio(String bucket, String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }

    private Image getConfirmedImage(String imageId) {
        return imageRepository.findByIdAndStatus(imageId, Image.ImageStatus.CONFIRMED)
                .orElseThrow(() -> new ImageNotFoundException(
                        "Confirmed image not found: " + imageId));
    }

    private void confirmThumbnail(String thumbnailImageId) {
        imageRepository.findById(thumbnailImageId)
                .ifPresent(thumbnail -> {
                    thumbnail.setStatus(Image.ImageStatus.CONFIRMED);
                    thumbnail.setConfirmedAt(Instant.now());
                    imageRepository.save(thumbnail);
                });
    }

    private void deleteThumbnail(String thumbnailImageId) {
        imageRepository.findById(thumbnailImageId)
                .ifPresent(thumbnail -> {
                    try {
                        removeObjectFromMinio(thumbnail.getBucketName(), thumbnail.getObjectName());
                        thumbnail.setStatus(Image.ImageStatus.DELETED);
                        imageRepository.save(thumbnail);
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

        return imageMapper.toConfirmUploadResponse(record, imageUrl, thumbnailUrl, confirmed);
    }
}