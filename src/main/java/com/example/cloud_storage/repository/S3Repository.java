package com.example.cloud_storage.repository;

import com.example.cloud_storage.dto.resource.ResourceInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface S3Repository {
    void uploadFile(String path, MultipartFile file);

    InputStream downloadFile(String path);

    void delete(String path);

    void copy(String fromPath, String toPath);

    List<String> listDirectory(String path);

    boolean exists(String path);

    void createDirectory(String path);

    Long getFileSize(String path);

    List<String> listDirectoryRecursive(String path);

    List<ResourceInfo> listAllObjectsRecursive(String fullPrefix);
}