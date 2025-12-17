package ru.tishembitov.pictorium.image;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Content type is required")
    private String contentType;

    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be greater than 0")
    private Long fileSize;

    private String category;

    @Builder.Default
    private boolean generateThumbnail = false;

    @Min(value = 50, message = "Thumbnail width must be at least 50")
    @Max(value = 1000, message = "Thumbnail width must not exceed 1000")
    private Integer thumbnailWidth;

    @Min(value = 50, message = "Thumbnail height must be at least 50")
    @Max(value = 1000, message = "Thumbnail height must not exceed 1000")
    private Integer thumbnailHeight;
}