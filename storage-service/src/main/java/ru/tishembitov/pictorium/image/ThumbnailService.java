package ru.tishembitov.pictorium.image;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.exception.ImageStorageException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private static final int DEFAULT_WIDTH = 236;
    private static final int DEFAULT_HEIGHT = 350;
    private static final double DEFAULT_QUALITY = 0.85;

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    @PostConstruct
    public void init() {
        String[] readers = ImageIO.getReaderFormatNames();
        String[] writers = ImageIO.getWriterFormatNames();

        log.info("Supported image readers: {}", Arrays.toString(readers));
        log.info("Supported image writers: {}", Arrays.toString(writers));

        // Проверяем критичные форматы
        boolean webpSupported = Arrays.stream(readers)
                .anyMatch(r -> r.equalsIgnoreCase("webp"));

        if (!webpSupported) {
            log.warn("WebP format is NOT supported! Add TwelveMonkeys dependency.");
        } else {
            log.info("WebP format is supported");
        }
    }

    public boolean isFormatSupported(String contentType) {
        if (contentType == null) {
            return false;
        }
        return SUPPORTED_FORMATS.contains(contentType.toLowerCase());
    }


    public byte[] generateThumbnail(InputStream originalImage,
                                    Integer maxWidth,
                                    Integer maxHeight) {
        int targetWidth = maxWidth != null && maxWidth > 0 ? maxWidth : DEFAULT_WIDTH;
        int targetHeight = maxHeight != null && maxHeight > 0 ? maxHeight : DEFAULT_HEIGHT;

        BufferedImage bufferedImage = null;

        try {
            bufferedImage = ImageIO.read(originalImage);

            if (bufferedImage == null) {
                throw new ImageStorageException(
                        "Cannot read image. Format may not be supported. " +
                                "Supported readers: " + String.join(", ", ImageIO.getReaderFormatNames()));
            }

            log.debug("Original image: {}x{}, generating thumbnail with max: {}x{}",
                    bufferedImage.getWidth(), bufferedImage.getHeight(),
                    targetWidth, targetHeight);

            int[] dimensions = calculateDimensions(
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight(),
                    targetWidth,
                    targetHeight
            );

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Thumbnails.of(bufferedImage)
                        .size(dimensions[0], dimensions[1])
                        .keepAspectRatio(true)
                        .outputFormat("jpg")
                        .outputQuality(DEFAULT_QUALITY)
                        .toOutputStream(outputStream);

                byte[] thumbnailBytes = outputStream.toByteArray();

                log.debug("Generated thumbnail: {}x{}, size: {} bytes",
                        dimensions[0], dimensions[1], thumbnailBytes.length);

                return thumbnailBytes;
            }

        } catch (ImageStorageException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to generate thumbnail: {}", e.getMessage(), e);
            throw new ImageStorageException("Failed to generate thumbnail: " + e.getMessage(), e);
        } finally {
            if (bufferedImage != null) {
                bufferedImage.flush();
            }
        }
    }


    private int[] calculateDimensions(int originalWidth, int originalHeight,
                                      int maxWidth, int maxHeight) {
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        if (ratio > 1.0) {
            ratio = 1.0;
        }

        int newWidth = (int) Math.round(originalWidth * ratio);
        int newHeight = (int) Math.round(originalHeight * ratio);

        newWidth = Math.max(newWidth, 1);
        newHeight = Math.max(newHeight, 1);

        return new int[]{newWidth, newHeight};
    }
}