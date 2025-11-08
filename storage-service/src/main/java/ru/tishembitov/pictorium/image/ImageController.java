package ru.tishembitov.pictorium.image;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageServiceImpl imageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImage(@ModelAttribute ImageUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imageService.uploadImage(request));
    }

    @GetMapping("/{imageId}")
    public void download(@PathVariable String imageId, HttpServletResponse response) {
        try {
            InputStream imageStream = imageService.getImage(imageId);
            ImageMetadata metadata = imageService.getImageMetadata(imageId);

            response.setContentType(metadata.getContentType());
            response.setHeader("Content-Disposition", "inline; filename=\"" + metadata.getFileName() + "\"");

            IOUtils.copy(imageStream, response.getOutputStream());
            response.flushBuffer();

        } catch (Exception e) {
            log.error("Error getting image: {}", imageId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{imageId}/url")
    public ResponseEntity<String> getImageUrl(
            @PathVariable String imageId,
            @RequestParam(value = "expiry", required = false) Integer expirySeconds) {

        return ResponseEntity.ok(imageService.getImageUrl(imageId, expirySeconds));
    }

    @GetMapping("/{imageId}/metadata")
    public ResponseEntity<ImageMetadata> getImageMetadata(@PathVariable String imageId) {
        return ResponseEntity.ok(imageService.getImageMetadata(imageId));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable String imageId) {
        imageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<ImageMetadata>> listImages(@RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(imageService.listImagesByCategory(category));
    }
}
