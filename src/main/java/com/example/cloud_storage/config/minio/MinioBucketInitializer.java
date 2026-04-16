package com.example.cloud_storage.config.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @PostConstruct
    public void init() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .build()
                );
                log.info("✅ Bucket '{}' created successfully", minioProperties.getBucketName());
            } else {
                log.info("✅ Bucket '{}' already exists", minioProperties.getBucketName());
            }
        } catch (Exception e) {
            log.error("❌ Failed to create bucket '{}': {}", minioProperties.getBucketName(), e.getMessage());
        }
    }
}