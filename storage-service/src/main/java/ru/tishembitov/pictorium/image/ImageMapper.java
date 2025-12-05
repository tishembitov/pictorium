package ru.tishembitov.pictorium.image;

import org.mapstruct.*;

import java.time.Instant;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {Instant.class}
)
public interface ImageMapper {

    @Mapping(target = "id", source = "imageId")
    @Mapping(target = "objectName", source = "objectName")
    @Mapping(target = "bucketName", source = "bucketName")
    @Mapping(target = "fileName", source = "request.fileName")
    @Mapping(target = "contentType", source = "request.contentType")
    @Mapping(target = "fileSize", source = "request.fileSize")
    @Mapping(target = "category", source = "request.category")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "thumbnailImageId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    Image toImageEntity(
            PresignedUploadRequest request,
            String imageId,
            String objectName,
            String bucketName
    );

    @Mapping(target = "id", source = "thumbnailImageId")
    @Mapping(target = "objectName", source = "thumbnailObjectName")
    @Mapping(target = "bucketName", source = "thumbnailBucket")
    @Mapping(target = "fileName", expression = "java(\"thumb_\" + request.getFileName())")
    @Mapping(target = "contentType", constant = "image/jpeg")
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "category", source = "request.category")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "thumbnailImageId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    Image toThumbnailEntity(
            PresignedUploadRequest request,
            String thumbnailImageId,
            String thumbnailObjectName,
            String thumbnailBucket
    );

    @Mapping(target = "imageId", source = "id")
    @Mapping(target = "lastModified", expression = "java(getLastModifiedMillis(image))")
    @Mapping(target = "etag", ignore = true)
    ImageMetadata toImageMetadata(Image image);


    List<ImageMetadata> toImageMetadataList(List<Image> images);

    @Mapping(target = "imageId", source = "image.id")
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "thumbnailUrl", source = "thumbnailUrl")
    @Mapping(target = "fileName", source = "image.fileName")
    @Mapping(target = "size", source = "image.fileSize")
    @Mapping(target = "contentType", source = "image.contentType")
    @Mapping(target = "uploadTimestamp", expression = "java(getUploadTimestamp(image))")
    @Mapping(target = "confirmed", source = "confirmed")
    ConfirmUploadResponse toConfirmUploadResponse(
            Image image,
            String imageUrl,
            String thumbnailUrl,
            boolean confirmed
    );

    @Mapping(target = "imageId", source = "imageId")
    @Mapping(target = "uploadUrl", source = "uploadUrl")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "requiredHeaders", source = "requiredHeaders")
    @Mapping(target = "thumbnailImageId", source = "thumbnailImageId")
    @Mapping(target = "thumbnailUploadUrl", source = "thumbnailUploadUrl")
    @Mapping(target = "thumbnailObjectName", source = "thumbnailObjectName")
    PresignedUploadResponse toPresignedUploadResponse(
            String imageId,
            String uploadUrl,
            Long expiresAt,
            java.util.Map<String, String> requiredHeaders,
            String thumbnailImageId,
            String thumbnailUploadUrl,
            String thumbnailObjectName
    );

    @Mapping(target = "imageId", source = "imageId")
    @Mapping(target = "url", source = "url")
    @Mapping(target = "expiresAt", source = "expiresAt")
    ImageUrlResponse toImageUrlResponse(String imageId, String url, Long expiresAt);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "objectName", ignore = true)
    @Mapping(target = "bucketName", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "thumbnailImageId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", constant = "CONFIRMED")
    @Mapping(target = "confirmedAt", expression = "java(Instant.now())")
    void updateOnConfirm(
            @MappingTarget Image image,
            Long fileSize,
            String contentType,
            String fileName
    );
}