package ru.tishembitov.pictorium.image;

import org.mapstruct.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    @Mapping(target = "thumbnailWidth", source = "request.thumbnailWidth")
    @Mapping(target = "thumbnailHeight", source = "request.thumbnailHeight")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "thumbnailImageId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    Image toEntity(PresignedUploadRequest request, String imageId, String objectName, String bucketName);

    @Mapping(target = "imageId", source = "id")
    @Mapping(target = "size", source = "fileSize")
    @Mapping(target = "etag", ignore = true)
    ImageMetadata toMetadata(Image image);

    List<ImageMetadata> toMetadataList(List<Image> images);

    @Mapping(target = "imageId", source = "image.id")
    @Mapping(target = "size", source = "image.fileSize")
    @Mapping(target = "confirmed", expression = "java(image.isConfirmed())")
    ConfirmUploadResponse toConfirmResponse(Image image, String imageUrl, String thumbnailUrl);

    default PresignedUploadResponse toPresignedResponse(String imageId,
                                                        String uploadUrl,
                                                        long expiresAt,
                                                        Map<String, String> requiredHeaders,
                                                        String thumbnailImageId) {
        return PresignedUploadResponse.builder()
                .imageId(imageId)
                .uploadUrl(uploadUrl)
                .expiresAt(expiresAt)
                .requiredHeaders(requiredHeaders)
                .thumbnailImageId(thumbnailImageId)
                .build();
    }

    default ImageUrlResponse toUrlResponse(String imageId, String url, long expiresAt) {
        return ImageUrlResponse.builder()
                .imageId(imageId)
                .url(url)
                .expiresAt(expiresAt)
                .build();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "objectName", ignore = true)
    @Mapping(target = "bucketName", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "thumbnailImageId", ignore = true)
    @Mapping(target = "thumbnailWidth", ignore = true)
    @Mapping(target = "thumbnailHeight", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", constant = "CONFIRMED")
    @Mapping(target = "confirmedAt", expression = "java(Instant.now())")
    void updateOnConfirm(@MappingTarget Image image, Long fileSize, String contentType, String fileName);
}