package com.example.cloud_storage.service;

import com.example.cloud_storage.repository.S3Repository;
import com.example.cloud_storage.util.PathUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class DirectoryService {
    private final S3Repository s3Repository;

    public StreamingResponseBody downloadZip(String path) { //TODO: возможно нуждается в переделке из-за объекта-маркера
        return (OutputStream outputStream) -> {
            List<String> files = s3Repository.listDirectoryRecursive(path);
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                for (String filePath : files) {
                    String relativePath = PathUtil.getRelativePath(filePath, path);
                    zipOut.putNextEntry(new ZipEntry(relativePath));
                    try (InputStream fileStream = s3Repository.downloadFile(filePath)) {
                        fileStream.transferTo(zipOut);
                    }
                    zipOut.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create zip", e);
            }
        };
    }

    public void delete(String fullPath) {
        List<String> objects = s3Repository.listDirectoryRecursive(fullPath);
        for (String obj : objects) {
            s3Repository.deleteResource(obj);
        }
    }

    public void ensureDirectoriesForFile(String fullFilePath) {
        String parentPath = PathUtil.extractParentPath(fullFilePath);
        if (parentPath.isEmpty()) return;
        String[] parts = parentPath.split("/");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            current.append(part).append("/");
            String folder = current.toString();
            if (!exists(folder)) {
                s3Repository.createFolder(folder);
            }
        }
    }

    public boolean exists(String fullPath) {
        return s3Repository.resourceExists(fullPath);
    }

    public void move(String fullFromPath, String fullToPath) { //TODO: Применён паттерн Saga
        List<String> objects = s3Repository.listDirectoryRecursive(fullFromPath);
        List<String> copied = new ArrayList<>();

        try {
            for (String oldPath : objects) {
                String newPath = oldPath.replace(fullFromPath, fullToPath);
                s3Repository.copyResource(oldPath, newPath);
                copied.add(oldPath);
            }
            for (String oldPath : objects) {
                s3Repository.deleteResource(oldPath);
            }
        } catch (Exception e) {
            for (String oldPath : copied) {
                String newPath = oldPath.replace(fullFromPath, fullToPath);
                s3Repository.deleteResource(newPath);
            }
            throw new RuntimeException("Move failed, rolled back", e);
        }
    }
}
