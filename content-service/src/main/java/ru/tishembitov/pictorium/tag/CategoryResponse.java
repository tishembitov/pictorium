package ru.tishembitov.pictorium.tag;

import java.util.UUID;

public record CategoryResponse(
        UUID tagId,
        String tagName,
        PinPreview pin
) {
    public record PinPreview(
            UUID id,
            String imageId,
            String thumbnailId,
            String videoPreviewId
    ) {}
}