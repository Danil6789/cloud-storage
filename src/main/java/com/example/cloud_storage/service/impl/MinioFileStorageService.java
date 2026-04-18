package com.example.cloud_storage.service.impl;

import com.example.cloud_storage.exception.resource.S3OperationException;
import com.example.cloud_storage.service.FileStorageService;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.cloud_storage.config.minio.MinioProperties;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import com.example.cloud_storage.dto.resource.FileInfo;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {
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
            throw new S3OperationException("Failed to upload file", e);
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
            throw new S3OperationException("Failed to download file", e);
        }
    }

    @Override
    public void deleteResource(Long userId, String path) {
        String fullPath = getFullPath(userId, path);
        try {
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
            throw new S3OperationException("Failed to delete resource", e);
        }
    }

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
            throw new S3OperationException("Failed to delete directory", e);
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
            throw new S3OperationException("Failed to move resource", e);
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
            throw new S3OperationException("Failed to list directory", e);
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
        String folderPath = fullPath.endsWith("/") ? fullPath : fullPath + "/";

        try {
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
            throw new S3OperationException("Failed to create folder", e);
        }
    }

    @Override
    public Long getFileSize(Long userId, String path) {
        String fullPath = getFullPath(userId, path);
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullPath)
                            .build()
            );
            return stat.size();
        } catch (Exception e) {
            log.error("Failed to get file size: {}", fullPath, e);
            throw new S3OperationException("Failed to get file size", e);
        }
    }

    @Override
    public List<String> listDirectoryRecursive(Long userId, String path) {
        String fullPath = getFullPath(userId, path);
        List<String> objects = new ArrayList<>();

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
                String objectName = item.objectName();

                if (!objectName.endsWith("/")) {
                    String relativePath = objectName.replace("user-" + userId + "-files/", "");
                    objects.add(relativePath);
                }
            }

            return objects;
        } catch (Exception e) {
            log.error("Failed to list directory recursively: {}", fullPath, e);
            throw new S3OperationException("Failed to list directory", e);
        }
    }

    @Override
    public List<FileInfo> listAllFilesRecursive(Long userId, String path) {
        String fullPath = getFullPath(userId, path);
        List<FileInfo> files = new ArrayList<>();

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
                String objectName = item.objectName();

                if (!objectName.endsWith("/")) {
                    String relativePath = objectName.replace("user-" + userId + "-files/", "");
                    files.add(FileInfo.builder()
                            .path(relativePath)
                            .size(item.size())
                            .build());
                }
            }

            log.info("✅ Listed all files for user {}: {} items", userId, files.size());
            return files;
        } catch (Exception e) {
            log.error("❌ Failed to list all files for user {}", userId, e);
            throw new S3OperationException("Failed to list files", e);
        }
    }
}