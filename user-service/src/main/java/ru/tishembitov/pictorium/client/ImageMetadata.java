package ru.tishembitov.pictorium.client;

public record ImageMetadata(
        String imageId,
        String fileName,
        String contentType,
        Long size,
        String etag,
        Long lastModified,
        String bucketName
) {}
