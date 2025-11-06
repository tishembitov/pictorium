package ru.tishembitov.pictorium.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String thumbnailBucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @PostConstruct
    public void init() {
        try {
            MinioClient client = minioClient();

            createBucketIfNotExists(client, bucketName);

            if (thumbnailBucket != null && !thumbnailBucket.isEmpty()) {
                createBucketIfNotExists(client, thumbnailBucket);
            }

            log.info("MinIO buckets initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing MinIO buckets", e);
        }
    }

    private void createBucketIfNotExists(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );

        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket '{}' created", bucket);
        }
    }
}