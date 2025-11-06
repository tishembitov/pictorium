package ru.tishembitov.pictorium.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    private String imageId;
    private String imageUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long size;
    private String contentType;
    private Long uploadTimestamp;
}
