package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.ResourceFactory;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.exception.resource.ResourceAlreadyExistsException;
import com.example.cloud_storage.exception.resource.ResourceNotFoundException;
import com.example.cloud_storage.exception.resource.BadRequestException;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.S3Repository;
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
    private final PathService pathService;
    private final S3Repository s3Repository;
    private final FileService fileService;
    private final ResourceMapper resourceMapper;
    private final ResourceFactory resourceFactory;

    public ResourceResponse getInfoDirectory(Resource resource){ //TODO: либо сделать чтоб Resource resource передавалось как в FileService в getInfo() ну или наоборт сделать
        return resourceMapper.toResponseDto(resource, null); //TODO: и тоже нужна проверка что файл это или папка или сделать FileResource и DirectoryResource
    }

    public ResourceResponse createDirectory(String path){
        String fullPath = pathService.getFullPath(path);
        if (!exists(path)) {
            s3Repository.createDirectory(fullPath);
            Resource resource = resourceFactory.create(path);//TODO: переделать логику

            return getInfoDirectory(resource);
        }
        else{
            throw new ResourceAlreadyExistsException("Такая папка уже существует");
        }
    }

    public boolean exists(String fullPath) {
        return s3Repository.resourceExists(fullPath);
    }

    public List<ResourceResponse> getDirectoryContents(String path) {
        String fullPath = pathService.getFullPath(path);

        if (!exists(fullPath)) {
            throw new ResourceNotFoundException("Папка не найдена: " + path);
        }
        if (!fullPath.endsWith("/")) {
            throw new BadRequestException("Указанный путь не является папкой: " + path);
        }

        List<String> items = s3Repository.listDirectory(fullPath);

        return items.stream()
                .filter(itemKey -> !itemKey.equals(fullPath))
                .map(itemKey -> {
                    Resource resource = resourceFactory.create(pathService.getRelativePath(itemKey, fullPath));
                    return fileService.getInfo(resource);
                })
                .collect(Collectors.toList());
    }

    public void createUserDirectory() {
        createDirectory(pathService.getCurrentUserRootPath());
    }

    public StreamingResponseBody downloadZip(String path) { //TODO: возможно нуждается в переделке из-за объекта-маркера
        return (OutputStream outputStream) -> {
            List<String> files = s3Repository.listDirectoryRecursive(path);
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                for (String filePath : files) {
                    String relativePath = pathService.getRelativePath(filePath, path);
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
        if (!s3Repository.resourceExists(fullPath) &&
                s3Repository.listDirectory(fullPath).isEmpty()) {
            throw new ResourceNotFoundException("Папка не найдена: " + fullPath);
        }

        List<String> paths = s3Repository.listDirectoryRecursive(fullPath);
        for (String path : paths) {
            fileService.delete(path);
        }
    }

    public void ensureDirectoriesForFile(String fullFilePath) { //TODO: для создания пустых папок в котором хранится этот файл
        String parentPath = pathService.extractParentPath(fullFilePath);

        if (parentPath.isEmpty()) return;
        String[] parts = parentPath.split("/");
        StringBuilder current = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;
            current.append(part).append("/");
            String folder = current.toString();
            if (!exists(folder)) {
                s3Repository.createDirectory(folder);
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