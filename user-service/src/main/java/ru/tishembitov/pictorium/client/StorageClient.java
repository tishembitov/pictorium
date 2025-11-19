package ru.tishembitov.pictorium.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "storage-service", url = "${services.storage-service.url}")
public interface StorageClient {

    @GetMapping("/api/v1/images/{imageId}/url")
    String getImageUrl(@PathVariable String imageId,
                       @RequestParam(value = "expiry", required = false) Integer expirySeconds);

    @GetMapping("/api/v1/images/{imageId}/metadata")
    ImageMetadata getImageMetadata(@PathVariable String imageId);

    @DeleteMapping("/api/v1/images/{imageId}")
    void deleteImage(@PathVariable String imageId);
}