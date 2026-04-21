package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.ResourceFactory;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.exception.resource.ResourceAlreadyExistsException;
import com.example.cloud_storage.exception.resource.ResourceNotFoundException;
import com.example.cloud_storage.exception.resource.BadRequestException;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.S3Repository;
import com.example.cloud_storage.util.PathUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService {
    private final S3Repository s3Repository;
    private final FileService fileService;
    private final ResourceMapper resourceMapper;

    public ResourceResponse getInfoDirectory(Resource resource){ //TODO: либо сделать чтоб Resource resource передавалось как в FileService в getInfo() ну или наоборт сделать
        return resourceMapper.toResponseDto(resource, null); //TODO: и тоже нужна проверка что файл это или папка или сделать FileResource и DirectoryResource
    }

    public ResourceResponse createDirectory(Long userId, String path){
        String fullPath = PathUtil.getFullPath(userId, path);
        if (!s3Repository.resourceExists(path)) {
            s3Repository.createFolder(fullPath);
            Resource resource = ResourceFactory.create(userId, path);

            return getInfoDirectory(resource);
        }
        else{
            throw new ResourceAlreadyExistsException("Такая папка уже существует");
        }
    }

    public List<ResourceResponse> getDirectoryContents(Long userId, String path) {
        String fullPath = PathUtil.getFullPath(userId, path);

        if (!s3Repository.resourceExists(fullPath)) {
            throw new ResourceNotFoundException("Папка не найдена: " + path);
        }
        if (!fullPath.endsWith("/")) {
            throw new BadRequestException("Указанный путь не является папкой: " + path);
        }

        List<String> items = s3Repository.listDirectory(fullPath);

        return items.stream()
                .filter(itemKey -> !itemKey.equals(fullPath))
                .map(itemKey -> {
                    Resource resource = ResourceFactory.create(userId, PathUtil.getRelativePath(itemKey, fullPath));
                    return fileService.getInfo(resource);
                })
                .collect(Collectors.toList());
    }

    public void createUserDirectory(Long userId) {
        createDirectory(userId, "");
    }

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
            if (!s3Repository.resourceExists(folder)) {
                s3Repository.createFolder(folder);
            }
        }
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