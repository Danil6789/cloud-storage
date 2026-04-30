package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.ResourceFactory;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.exception.resource.*;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.resource.S3Repository;
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

import static com.example.cloud_storage.constant.ExceptionMessages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService {
    private final PathService pathService;
    private final S3Repository s3Repository;
    private final FileService fileService;
    private final ResourceMapper resourceMapper;
    private final ResourceFactory resourceFactory;

    public ResourceResponse getInfo(Resource resource){
        if(!s3Repository.exists(resource.fullPath())){
            throw new ResourceNotFoundException(RESOURCE_NOT_FOUND);
        }

        return resourceMapper.toResponseDto(resource, null);
    }

    public ResourceResponse createDirectory(String path){
        String fullPath = pathService.getFullPath(path);
        if (!s3Repository.exists(fullPath)) {
            s3Repository.createDirectory(fullPath);
            Resource resource = resourceFactory.create(path);

            return getInfo(resource);
        }
        else{
            throw new ResourceAlreadyExistsException(RESOURCE_ALREADY_EXISTS);
        }
    }

    public void createUserDirectory(Long userId) {
        String fullPath = "user-%d-files/".formatted(userId);
        if (!s3Repository.exists(fullPath)) {
            s3Repository.createDirectory(fullPath);
        }
        else{
            throw new ResourceAlreadyExistsException(RESOURCE_ALREADY_EXISTS);
        }
    }

    public List<ResourceResponse> getDirectoryContents(String path) {
        String fullPath = pathService.getFullPath(path);

        if (!isDirectoryExists(fullPath)) {
            throw new ResourceNotFoundException(RESOURCE_NOT_FOUND + " " + path);
        }
        if (!fullPath.endsWith("/")) {
            throw new BadRequestException(BAD_REQUEST + " " + path);
        }

        List<String> items = s3Repository.listDirectory(fullPath);

        return items.stream()
                .filter(itemKey -> !itemKey.equals(fullPath))
                .map(itemKey -> {
                    Resource resource = resourceFactory.create(pathService.getRelativePath(itemKey, pathService.getCurrentUserRootPath()));
                    if(resource.isDirectory()){
                        return getInfo(resource);
                    }
                    return fileService.getInfo(resource);
                })
                .collect(Collectors.toList());
    }

    private boolean isDirectoryExists(String fullPath) {
        if (s3Repository.exists(fullPath)) return true;
        List<String> children = s3Repository.listDirectory(fullPath);
        return !children.isEmpty();
    }

    public StreamingResponseBody download(String path) {
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
                throw new ServerIOException(SERVER_IO_ERROR, e);
            }
        };
    }

    public void delete(String fullPath) {
        if (!s3Repository.exists(fullPath) &&
                s3Repository.listDirectory(fullPath).isEmpty()) {
            throw new ResourceNotFoundException(RESOURCE_NOT_FOUND + " " + fullPath);
        }

        List<String> paths = s3Repository.listDirectoryRecursive(fullPath);
        for (String path : paths) {
            fileService.delete(path);
        }
    }

    public void ensureDirectoriesForFile(String fullFilePath) {
        String parentPath = pathService.extractParentPath(fullFilePath);

        if (parentPath.isEmpty()) return;
        String[] parts = parentPath.split("/");
        StringBuilder current = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;
            current.append(part).append("/");
            String folder = current.toString();
            if (!s3Repository.exists(folder)) {
                s3Repository.createDirectory(folder);
            }
        }
    }

    public void moveOrRename(String fullFromPath, String fullToPath) {
        List<String> objects = s3Repository.listDirectoryRecursive(fullFromPath);
        List<String> copied = new ArrayList<>();

        try {
            for (String oldPath : objects) {
                String newPath = oldPath.replace(fullFromPath, fullToPath);
                s3Repository.copy(oldPath, newPath);
                copied.add(oldPath);
            }
            for (String oldPath : objects) {
                s3Repository.delete(oldPath);
            }
        } catch (Exception e) {
            for (String oldPath : copied) {
                String newPath = oldPath.replace(fullFromPath, fullToPath);
                s3Repository.delete(newPath);
            }

            throw new MoveOperationException(MOVE_OPERATION_EXCEPTION, e);
        }
    }
}