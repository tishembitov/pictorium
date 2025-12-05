package ru.tishembitov.pictorium.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.exception.ImageStorageException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private static final int DEFAULT_WIDTH = 236;
    private static final int DEFAULT_HEIGHT = 350;
    private static final double DEFAULT_QUALITY = 0.85;

    public byte[] generateThumbnail(InputStream originalImage,
                                    Integer width,
                                    Integer height) {
        int targetWidth = width != null ? width : DEFAULT_WIDTH;
        int targetHeight = height != null ? height : DEFAULT_HEIGHT;

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Thumbnails.of(originalImage)
                    .size(targetWidth, targetHeight)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(DEFAULT_QUALITY)
                    .toOutputStream(outputStream);

            byte[] thumbnailBytes = outputStream.toByteArray();
            log.debug("Generated thumbnail: {}x{}, size: {} bytes",
                    targetWidth, targetHeight, thumbnailBytes.length);

            return thumbnailBytes;

        } catch (Exception e) {
            log.error("Failed to generate thumbnail", e);
            throw new ImageStorageException("Failed to generate thumbnail: " + e.getMessage(), e);
        }
    }

    public byte[] generateThumbnailByWidth(InputStream originalImage, int width) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Thumbnails.of(originalImage)
                    .width(width)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(DEFAULT_QUALITY)
                    .toOutputStream(outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate thumbnail by width", e);
            throw new ImageStorageException("Failed to generate thumbnail: " + e.getMessage(), e);
        }
    }
}