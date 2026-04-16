package com.example.cloud_storage.service.minio;

import com.example.cloud_storage.dto.resource.FileInfo;

import java.io.InputStream;
import java.util.List;

public interface FileStorageService {

    void uploadFile(Long userId, String path, InputStream inputStream, long size);

    InputStream downloadFile(Long userId, String path);

    void deleteResource(Long userId, String path);

    void moveResource(Long userId, String fromPath, String toPath);

    List<String> listDirectory(Long userId, String path);

    boolean resourceExists(Long userId, String path);

    void createFolder(Long userId, String path);

    Long getFileSize(Long userId, String path);

    List<String> listDirectoryRecursive(Long userId, String path);

    List<FileInfo> listAllFilesRecursive(Long userId, String path);
}
