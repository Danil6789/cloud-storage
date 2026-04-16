package com.example.cloud_storage.service;

import com.example.cloud_storage.dto.resource.ResourceDownloadResponse;
import com.example.cloud_storage.dto.resource.ResourceResponse;
import com.example.cloud_storage.dto.resource.ResourceType;
import com.example.cloud_storage.exception.resource.ResourceAlreadyExistsException;
import com.example.cloud_storage.exception.resource.ResourceNotFoundException;
import com.example.cloud_storage.service.minio.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.example.cloud_storage.util.PathUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.example.cloud_storage.dto.resource.FileInfo;
import org.springframework.web.multipart.MultipartFile;


@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceService {
    private final FileStorageService fileStorageService;

    public ResourceResponse getInfoResource(Long userId, String path){
        String normPath = PathUtil.normalizePath(path);

        boolean isDirectory = normPath.endsWith("/");

        String name = PathUtil.extractName(normPath);
        String parentPath = PathUtil.extractParentPath(normPath);

        Long size = null;
        if (!isDirectory) {
            size = fileStorageService.getFileSize(userId, normPath);
        }

        return ResourceResponse.builder()
                .path(parentPath)
                .name(name)
                .size(size)
                .type(isDirectory ? ResourceType.DIRECTORY : ResourceType.FILE)
                .build();
    }

    public ResourceDownloadResponse downloadResource(Long userId, String path) {
        String normPath = PathUtil.normalizePath(path);
        boolean isDirectory = normPath.endsWith("/");

        if (isDirectory) {
            return downloadDirectoryAsZip(userId, normPath, PathUtil.extractName(normPath));
        } else {
            return downloadFile(userId, normPath, PathUtil.extractName(normPath));
        }
    }

    private ResourceDownloadResponse downloadFile(Long userId, String path, String fileName) {
        InputStream inputStream = fileStorageService.downloadFile(userId, path);
        Long size = fileStorageService.getFileSize(userId, path);

        return ResourceDownloadResponse.builder()
                .inputStream(inputStream)
                .fileName(fileName)
                .isDirectory(false)
                .size(size)
                .build();
    }

    private ResourceDownloadResponse downloadDirectoryAsZip(Long userId, String path, String folderName) {
        try {
            List<String> files = fileStorageService.listDirectoryRecursive(userId, path);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);

            for (String filePath : files) {
                String relativePath = PathUtil.getRelativePath(filePath, path);

                InputStream fileStream = fileStorageService.downloadFile(userId, filePath);

                ZipEntry zipEntry = new ZipEntry(relativePath);
                zipOut.putNextEntry(zipEntry);

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    zipOut.write(buffer, 0, bytesRead);
                }

                zipOut.closeEntry();
                fileStream.close();
            }
            zipOut.close();

            return ResourceDownloadResponse.builder()
                    .inputStream(new ByteArrayInputStream(baos.toByteArray()))
                    .fileName(folderName + ".zip")
                    .isDirectory(true)
                    .size(baos.size())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create zip archive for directory: {}", path, e);
            throw new RuntimeException("Failed to create zip archive", e);
        }
    }

    public void deleteResource(Long userId, String path){
        fileStorageService.deleteResource(userId, path);
    }

    public ResourceResponse moveResource(Long userId, String fromPath, String toPath) {
        String normalizedFrom = PathUtil.normalizePath(fromPath);
        String normalizedTo = PathUtil.normalizePath(toPath);

        if (!fileStorageService.resourceExists(userId, normalizedFrom)) {
            throw new ResourceNotFoundException("Source resource not found: " + fromPath);
        }

        if (fileStorageService.resourceExists(userId, normalizedTo)) {
            throw new ResourceAlreadyExistsException("Target resource already exists: " + toPath);
        }

        String parentPath = PathUtil.extractParentPath(normalizedTo);
        if (!parentPath.isEmpty() && !fileStorageService.resourceExists(userId, parentPath)) {
            throw new ResourceNotFoundException("Target parent directory not found: " + parentPath);
        }

        fileStorageService.moveResource(userId, normalizedFrom, normalizedTo);

        return getInfoResource(userId, toPath);
    }

    public List<ResourceResponse> searchResources(Long userId, String query) {
        String normalizedQuery = query.toLowerCase();
        List<ResourceResponse> results = new ArrayList<>();

        var allFiles = fileStorageService.listAllFilesRecursive(userId, "");

        for (FileInfo file : allFiles) {
            String filePath = file.getPath();
            String fileName = PathUtil.extractName(filePath);

            if (fileName.toLowerCase().contains(normalizedQuery)) {
                String normPath = PathUtil.normalizePath(filePath);
                boolean isDirectory = normPath.endsWith("/");

                String name = PathUtil.extractName(normPath);
                String parentPath = PathUtil.extractParentPath(normPath);

                ResourceResponse response = ResourceResponse.builder()
                        .path(parentPath)
                        .name(name)
                        .size(file.getSize())
                        .type(isDirectory ? ResourceType.DIRECTORY : ResourceType.FILE)
                        .build();

                results.add(response);
            }
        }

        log.info("Search completed. Query: '{}', Results: {}", query, results.size());
        return results;
    }

    public List<ResourceResponse> uploadFiles(Long userId, String path, List<MultipartFile> files) {
        String normalizedPath = PathUtil.normalizePath(path);
        List<ResourceResponse> responses = new ArrayList<>();

        String targetPath = normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/";

        if (!targetPath.isEmpty() && !fileStorageService.resourceExists(userId, targetPath)) {
            throw new ResourceNotFoundException("Target directory not found: " + path);
        }

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                throw new IllegalArgumentException("File name cannot be empty");
            }

            String fullFilePath = targetPath + originalFilename;

            createParentDirectories(userId, fullFilePath);

            if (fileStorageService.resourceExists(userId, fullFilePath)) {
                throw new ResourceAlreadyExistsException("File already exists: " + fullFilePath);
            }

            try {
                fileStorageService.uploadFile(userId, fullFilePath, file.getInputStream(), file.getSize());

                ResourceResponse response = getInfoResource(userId, fullFilePath);
                responses.add(response);
            } catch (IOException e) {
                log.error("Failed to upload file: {}", originalFilename, e);
                throw new RuntimeException("Failed to upload file: " + originalFilename, e);
            }
        }

        return responses;
    }

    private void createParentDirectories(Long userId, String filePath) {
        String parentPath = PathUtil.extractParentPath(filePath);
        if (parentPath.isEmpty()) {
            return;
        }

        String[] parts = parentPath.split("/");
        String currentPath = "";

        for (String part : parts) {
            if (part.isEmpty()) continue;
            currentPath += part + "/";

            if (!fileStorageService.resourceExists(userId, currentPath)) {
                fileStorageService.createFolder(userId, currentPath);
            }
        }
    }
}