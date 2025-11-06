package ru.tishembitov.pictorium.image;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import ru.tishembitov.pictorium.config.MinioConfig;
import ru.tishembitov.pictorium.exception.ImageNotFoundException;
import ru.tishembitov.pictorium.exception.ImageStorageException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Value("${image-storage.max-file-size:10485760}")  // 10MB по умолчанию
    private Long maxFileSize;

    @Value("${image-storage.allowed-content-types:image/jpeg,image/jpg,image/png,image/gif,image/webp}")
    private Set<String> allowedContentTypes;

    @Value("${image-storage.url-expiry-days:7}")  // 7 дней по умолчанию
    private Integer urlExpiryDays;

    @Override
    public ImageUploadResponse uploadImage(ImageUploadRequest request) {
        MultipartFile file = request.getFile();
        String category = request.getCategory();
        boolean generateThumbnail = request.isGenerateThumbnail();
        Integer thumbnailWidth = request.getThumbnailWidth();
        Integer thumbnailHeight = request.getThumbnailHeight();

        validateImage(file);

        try {
            String imageId = generateImageId();
            String fileName = file.getOriginalFilename();
            String extension = getFileExtension(fileName);
            String objectName = buildObjectName(imageId, category, extension);

            uploadToMinio(
                    minioConfig.getBucketName(),
                    objectName,
                    file.getInputStream(),
                    file.getContentType(),
                    file.getSize()
            );

            String imageUrl = getPresignedUrl(minioConfig.getBucketName(), objectName);
            String thumbnailUrl = null;

            if (generateThumbnail && minioConfig.getThumbnailBucket() != null) {
                thumbnailUrl = createAndUploadThumbnail(
                        file.getInputStream(),
                        objectName,
                        thumbnailWidth,
                        thumbnailHeight
                );
            }

            log.info("Image uploaded successfully: {} (size: {} bytes, category: {})",
                    imageId, file.getSize(), category);

            return new ImageUploadResponse(
                    imageId,
                    imageUrl,
                    thumbnailUrl,
                    fileName,
                    file.getSize(),
                    file.getContentType(),
                    System.currentTimeMillis()
            );

        } catch (Exception e) {
            log.error("Failed to upload image", e);
            throw new ImageStorageException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getImage(String imageId) {
        try {
            String objectName = findObjectByImageId(imageId);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get image: {}", imageId, e);
            throw new ImageNotFoundException("Image not found: " + imageId);
        }
    }

    @Override
    public String getImageUrl(String imageId, Integer expirySeconds) {
        try {
            String objectName = findObjectByImageId(imageId);

            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucketName())
                    .object(objectName);

            if (expirySeconds != null && expirySeconds > 0) {
                builder.expiry(expirySeconds);
            } else {
                builder.expiry(urlExpiryDays * 24 * 60 * 60);
            }

            return minioClient.getPresignedObjectUrl(builder.build());
        } catch (Exception e) {
            log.error("Failed to get image URL: {}", imageId, e);
            throw new ImageStorageException("Failed to get image URL: " + e.getMessage(), e);
        }
    }

    @Override
    public ImageMetadata getImageMetadata(String imageId) {
        try {
            String objectName = findObjectByImageId(imageId);

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );

            return new ImageMetadata(
                    imageId,
                    extractFileName(objectName),
                    stat.contentType(),
                    stat.size(),
                    stat.etag(),
                    stat.lastModified().toInstant().toEpochMilli(),
                    minioConfig.getBucketName()
            );

        } catch (Exception e) {
            log.error("Failed to get image metadata: {}", imageId, e);
            throw new ImageNotFoundException("Image not found: " + imageId);
        }
    }

    @Override
    public void deleteImage(String imageId) {
        try {
            String objectName = findObjectByImageId(imageId);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );

            if (minioConfig.getThumbnailBucket() != null) {
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(minioConfig.getThumbnailBucket())
                                    .object(objectName)
                                    .build()
                    );
                } catch (Exception e) {
                    log.warn("Failed to delete thumbnail for image: {}", imageId);
                }
            }

            log.info("Image deleted successfully: {}", imageId);

        } catch (Exception e) {
            log.error("Failed to delete image: {}", imageId, e);
            throw new ImageStorageException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ImageMetadata> listImagesByCategory(String category) {
        try {
            String prefix = category != null ? category + "/" : "";

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            return StreamSupport.stream(results.spliterator(), false)
                    .map(itemResult -> {
                        try {
                            Item item = itemResult.get();
                            return new ImageMetadata(
                                    extractImageId(item.objectName()),
                                    extractFileName(item.objectName()),
                                    null,
                                    item.size(),
                                    item.etag(),
                                    item.lastModified().toInstant().toEpochMilli(),
                                    minioConfig.getBucketName()
                            );
                        } catch (Exception e) {
                            log.error("Failed to process item", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to list images by category: {}", category, e);
            throw new ImageStorageException("Failed to list images: " + e.getMessage(), e);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageStorageException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new ImageStorageException(
                    String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                            file.getSize(), maxFileSize)
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase())) {
            throw new ImageStorageException(
                    "Invalid file type. Allowed types: " + allowedContentTypes
            );
        }
    }

    private String generateImageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String getFileExtension(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return "jpg";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "jpg";
    }

    private String buildObjectName(String imageId, String category, String extension) {
        StringBuilder builder = new StringBuilder();

        // Категория (опционально)
        if (StringUtils.isNotEmpty(category)) {
            builder.append(category).append("/");
        }

        // Дата для организации файлов
        builder.append(ZonedDateTime.now().toLocalDate().toString()).append("/");

        // ID и расширение
        builder.append(imageId).append(".").append(extension);

        return builder.toString();
    }

    private void uploadToMinio(String bucket, String objectName,
                               InputStream inputStream, String contentType,
                               long size) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build()
        );
    }

    private String createAndUploadThumbnail(InputStream inputStream, String objectName,
                                            Integer width, Integer height) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(inputStream)
                .size(width, height)
                .outputFormat("jpg")
                .toOutputStream(outputStream);

        byte[] thumbnailBytes = outputStream.toByteArray();
        ByteArrayInputStream thumbnailStream = new ByteArrayInputStream(thumbnailBytes);

        uploadToMinio(
                minioConfig.getThumbnailBucket(),
                objectName,
                thumbnailStream,
                "image/jpeg",
                thumbnailBytes.length
        );

        return getPresignedUrl(minioConfig.getThumbnailBucket(), objectName);
    }

    private String getPresignedUrl(String bucket, String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectName)
                        .expiry(urlExpiryDays * 24 * 60 * 60)  // Из конфигурации
                        .build()
        );
    }

    private String findObjectByImageId(String imageId) throws Exception {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            if (item.objectName().contains(imageId)) {
                return item.objectName();
            }
        }

        throw new ImageNotFoundException("Image not found with ID: " + imageId);
    }

    private String extractImageId(String objectName) {
        String fileName = objectName.substring(objectName.lastIndexOf('/') + 1);
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private String extractFileName(String objectName) {
        return objectName.substring(objectName.lastIndexOf('/') + 1);
    }
}