package ru.tishembitov.pictorium.image;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.exception.ImageStorageException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

@Slf4j
@Service
public class ThumbnailService {

    private static final int DEFAULT_WIDTH = 236;
    private static final int DEFAULT_HEIGHT = 350;
    private static final double DEFAULT_QUALITY = 0.85;

    @PostConstruct
    public void init() {
        String[] readers = ImageIO.getReaderFormatNames();
        String[] writers = ImageIO.getWriterFormatNames();

        log.info("Supported image readers: {}", Arrays.toString(readers));
        log.info("Supported image writers: {}", Arrays.toString(writers));

        boolean webpReadSupported = Arrays.asList(readers).contains("webp")
                || Arrays.asList(readers).contains("WEBP");
        log.info("WebP read support: {}", webpReadSupported);
    }

    public byte[] generateThumbnail(InputStream originalImage,
                                    Integer width,
                                    Integer height) {
        int targetWidth = width != null ? width : DEFAULT_WIDTH;
        int targetHeight = height != null ? height : DEFAULT_HEIGHT;

        try {
            BufferedImage bufferedImage = ImageIO.read(originalImage);

            if (bufferedImage == null) {
                throw new ImageStorageException(
                        "Cannot read image. Format may not be supported. " +
                                "Supported: " + String.join(", ", ImageIO.getReaderFormatNames()));
            }

            log.debug("Original image: {}x{}",
                    bufferedImage.getWidth(), bufferedImage.getHeight());

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Thumbnails.of(bufferedImage)
                        .size(targetWidth, targetHeight)
                        .keepAspectRatio(true)
                        .outputFormat("jpg")
                        .outputQuality(DEFAULT_QUALITY)
                        .toOutputStream(outputStream);

                byte[] thumbnailBytes = outputStream.toByteArray();
                log.debug("Generated thumbnail: {}x{} target, {} bytes",
                        targetWidth, targetHeight, thumbnailBytes.length);

                return thumbnailBytes;
            }

        } catch (ImageStorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate thumbnail", e);
            throw new ImageStorageException("Failed to generate thumbnail: " + e.getMessage(), e);
        }
    }

    public byte[] generateThumbnailByWidth(InputStream originalImage, int width) {
        try {
            BufferedImage bufferedImage = ImageIO.read(originalImage);

            if (bufferedImage == null) {
                throw new ImageStorageException("Cannot read image");
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Thumbnails.of(bufferedImage)
                        .width(width)
                        .keepAspectRatio(true)
                        .outputFormat("jpg")
                        .outputQuality(DEFAULT_QUALITY)
                        .toOutputStream(outputStream);

                return outputStream.toByteArray();
            }

        } catch (ImageStorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate thumbnail by width", e);
            throw new ImageStorageException("Failed to generate thumbnail: " + e.getMessage(), e);
        }
    }
}