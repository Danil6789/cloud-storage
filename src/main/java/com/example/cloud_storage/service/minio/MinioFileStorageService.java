package com.example.cloud_storage.service.minio;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.cloud_storage.config.minio.MinioProperties;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService{
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    private String getFullPath(Long userId, String path) {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return String.format("user-%d-files/%s", userId, normalizedPath);
    }

    @Override
    public void uploadFile(Long userId, String path, InputStream inputStream, long size) {
        String fullPath = getFullPath(userId, path);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullPath)
                            .stream(inputStream, size, -1)
                            .contentType("application/octet-stream")
                            .build()
            );
            log.info("✅ File uploaded: {}", fullPath);
        } catch (Exception e) {
            log.error("❌ Failed to upload file: {}", fullPath, e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public InputStream downloadFile(Long userId, String path) {
        String fullPath = getFullPath(userId, path);
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullPath)
                            .build()
            );
            log.info("✅ File downloaded: {}", fullPath);
            return stream;
        } catch (Exception e) {
            log.error("❌ Failed to download file: {}", fullPath, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    @Override
    public void deleteResource(Long userId, String path) {
        String fullPath = getFullPath(userId, path);
        try {
            // Если путь заканчивается на / — это папка, удаляем всё с этим префиксом
            if (path.endsWith("/")) {
                deleteDirectory(userId, path);
            } else {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(fullPath)
                                .build()
                );
                log.info("✅ File deleted: {}", fullPath);
            }
        } catch (Exception e) {
            log.error("❌ Failed to delete resource: {}", fullPath, e);
            throw new RuntimeException("Failed to delete resource", e);
        }
    }

    /**
     * Удалить папку (все объекты с префиксом)
     */
    private void deleteDirectory(Long userId, String path) {
        String fullPath = getFullPath(userId, path);
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .prefix(fullPath)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(item.objectName())
                                .build()
                );
            }
            log.info("✅ Directory deleted: {}", fullPath);
        } catch (Exception e) {
            log.error("❌ Failed to delete directory: {}", fullPath, e);
            throw new RuntimeException("Failed to delete directory", e);
        }
    }

    @Override
    public void moveResource(Long userId, String fromPath, String toPath) {
        String fullFromPath = getFullPath(userId, fromPath);
        String fullToPath = getFullPath(userId, toPath);

        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullToPath)
                            .source(CopySource.builder()
                                    .bucket(minioProperties.getBucketName())
                                    .object(fullFromPath)
                                    .build())
                            .build()
            );

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullFromPath)
                            .build()
            );

            log.info("✅ Resource moved: {} → {}", fullFromPath, fullToPath);
        } catch (Exception e) {
            log.error("❌ Failed to move resource: {} → {}", fullFromPath, fullToPath, e);
            throw new RuntimeException("Failed to move resource", e);
        }
    }

    @Override
    public List<String> listDirectory(Long userId, String path) {
        String fullPath = getFullPath(userId, path);
        List<String> objects = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .prefix(fullPath)
                            .recursive(false)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                objects.add(item.objectName());
            }
            log.info("✅ Listed directory: {} ({} items)", fullPath, objects.size());
            return objects;
        } catch (Exception e) {
            log.error("❌ Failed to list directory: {}", fullPath, e);
            throw new RuntimeException("Failed to list directory", e);
        }
    }

    @Override
    public boolean resourceExists(Long userId, String path) {
        String fullPath = getFullPath(userId, path);
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullPath)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void createFolder(Long userId, String path) {
        String fullPath = getFullPath(userId, path);
        // Убедимся, что путь заканчивается на /
        String folderPath = fullPath.endsWith("/") ? fullPath : fullPath + "/";

        try {
            // Создаём пустой объект с именем папки (слеш в конце)
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(folderPath)
                            .stream(new InputStream() {
                                @Override
                                public int read() {
                                    return -1;
                                }
                            }, 0, -1)
                            .build()
            );
            log.info("✅ Folder created: {}", folderPath);
        } catch (Exception e) {
            log.error("❌ Failed to create folder: {}", folderPath, e);
            throw new RuntimeException("Failed to create folder", e);
        }
    }
}