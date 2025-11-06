package ru.tishembitov.pictorium.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageMetadata {

    private String imageId;
    private String fileName;
    private String contentType;
    private Long size;
    private String etag;
    private Long lastModified;
    private String bucketName;
}
