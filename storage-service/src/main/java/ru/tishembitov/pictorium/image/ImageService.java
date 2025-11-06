package ru.tishembitov.pictorium.image;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface ImageService {

    ImageUploadResponse uploadImage(ImageUploadRequest request);

    InputStream getImage(String imageId);

    ImageMetadata getImageMetadata(String imageId);

    String getImageUrl(String imageId, Integer expirySeconds);

    void deleteImage(String imageId);

    List<ImageMetadata> listImagesByCategory(String category);
}

