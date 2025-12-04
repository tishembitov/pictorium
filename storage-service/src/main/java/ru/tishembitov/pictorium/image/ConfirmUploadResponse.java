package ru.tishembitov.pictorium.image;

import lombok.*;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmUploadResponse {

    private String imageId;
    private String imageUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long size;
    private String contentType;
    private Long uploadTimestamp;
    private boolean confirmed;
}