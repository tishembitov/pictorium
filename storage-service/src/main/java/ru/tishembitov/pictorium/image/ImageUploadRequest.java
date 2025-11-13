package ru.tishembitov.pictorium.image;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ImageUploadRequest {

    @NotNull(message = "File is required")
    private MultipartFile file;

    private String category;

    private boolean generateThumbnail = false;

    private Integer thumbnailWidth = 200;

    private Integer thumbnailHeight = 200;
}
