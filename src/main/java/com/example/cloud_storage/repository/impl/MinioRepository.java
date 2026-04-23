package com.example.cloud_storage.repository.impl;

import com.example.cloud_storage.exception.resource.ResourceNotFoundException;
import com.example.cloud_storage.exception.resource.S3OperationException;
import com.example.cloud_storage.repository.S3Repository;
import com.example.cloud_storage.dto.resource.ResourceInfo;
import com.example.cloud_storage.exception.resource.ServerIOException;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;

import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.cloud_storage.config.minio.MinioProperties;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioRepository implements S3Repository {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void uploadFile(String fullPath, MultipartFile file) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/octet-stream")
                            .build()
            );
        } catch (MinioException e) {
            throw new S3OperationException("Failed to upload file", e);
        }catch(Exception e){
            throw new ServerIOException("Unexpected error during upload", e);
        }
    }

    @Override
    public InputStream downloadFile(String fullPath) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullPath)
                            .build()
            );
            return stream;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new ResourceNotFoundException("File not found: " + fullPath);
            }
            throw new S3OperationException("Failed to download file", e);
        } catch (Exception e) {
            throw new S3OperationException("Failed to download file", e);
        }
    }

    @Override
    public void delete(String fullPath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullPath)
                            .build()
            );
        } catch (Exception e) {
            throw new S3OperationException("Failed to delete resource", e);
        }
    }

    @Override
    public void copy(String fullFromPath, String fullToPath) {
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
        } catch (Exception e) {
            throw new S3OperationException("Failed to move resource", e);
        }
    }

    @Override
    public List<String> listDirectory(String fullPath) {
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
            return objects;
        } catch (Exception e) {
            throw new S3OperationException("Failed to list directory", e);
        }
    }

    @Override
    public boolean exists(String fullPath) {
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
    public void createDirectory(String fullPath) {
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
        } catch (Exception e) {
            throw new S3OperationException("Failed to create folder", e);
        }
    }

    @Override
    public Long getFileSize(String fullPath) {
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
    public List<String> listDirectoryRecursive(String prefix) {
        List<String> objects = new ArrayList<>();
        try {
            for (Result<Item> result : listObjects(prefix)) {
                Item item = result.get();
                objects.add(item.objectName());
            }
        } catch (Exception e) {
            throw new S3OperationException("Failed to list directory recursively", e);
        }
        return objects;
    }

    @Override
    public List<ResourceInfo> listAllObjectsRecursive(String fullPrefix) {
        List<ResourceInfo> objects = new ArrayList<>();
        try {
            for (Result<Item> result : listObjects(fullPrefix)) {
                Item item = result.get();
                String relativePath = item.objectName().substring(fullPrefix.length());
                objects.add(new ResourceInfo(relativePath, item.size()));
            }
        } catch (Exception e) {
            throw new S3OperationException("Failed to list objects recursively", e);
        }
        return objects;
    }

    private Iterable<Result<Item>> listObjects(String prefix) {
        try {
            return minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
        } catch (Exception e) {
            throw new S3OperationException("Failed to list objects", e);
        }
    }
}