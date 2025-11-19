package ru.tishembitov.pictorium.image;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ImageUploadRequest {

    @NotNull(message = "File is required")
    private MultipartFile file;

    private String category = "PIN";

    private boolean generateThumbnail = true;

    private Integer thumbnailWidth = 400;

    private Integer thumbnailHeight = 400;

    private String userId;
}
