package com.example.cloud_storage.service.impl;

import com.example.cloud_storage.dto.resource.*;
import com.example.cloud_storage.dto.resource.response.DownloadResponse;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.exception.resource.ResourceAlreadyExistsException;
import com.example.cloud_storage.exception.resource.ResourceNotFoundException;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.S3Repository;
import com.example.cloud_storage.service.resource.DirectoryService;
import com.example.cloud_storage.service.resource.FileService;
import com.example.cloud_storage.service.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.cloud_storage.dto.resource.ResourceInfo;
import org.springframework.stereotype.Service;
import com.example.cloud_storage.util.PathUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;


@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final S3Repository storageRepository; //TODO: Сделать так чтоб вызывалось только в fileService и directoryService
    private final ResourceMapper resourceMapper;
    private final FileService fileService; //TODO: Сделать чтоб указывался его интерфейс(надо создать его) чтоб соблюдался принцип DIP
    private final DirectoryService directoryService; //TODO: Сделать чтоб указывался его интерфейс(надо создать его) чтоб соблюдался принцип DIP

    @Override
    public ResourceResponse getInfoResource(Long userId, String path){//TODO: Всё таки надо сделать чтоб было разделение на FileService и DirectoryService
        Resource resource = ResourceFactory.create(userId, path);
        if(resource.isDirectory()){
            return directoryService.getInfoDirectory(resource);
        }
        else{
            return fileService.getInfo(resource);
        }
    }

    @Override
    public DownloadResponse downloadResource(Long userId, String path) {
        Resource resource = ResourceFactory.create(userId, path);
        String fileName = resource.name();

        if (resource.isDirectory()) {
            StreamingResponseBody body = directoryService.downloadZip(resource.fullPath());
            return new DownloadResponse(body, fileName + ".zip", true);

        } else {
            StreamingResponseBody body = fileService.downloadStream(resource.fullPath());
            return new DownloadResponse(body, fileName, false);
        }
    }

    @Override
    public void deleteResource(Long userId, String path){
        Resource resource = ResourceFactory.create(userId, path);
        if (resource.isDirectory()) {
            directoryService.delete(resource.fullPath());
        } else {
            fileService.delete(resource.fullPath());
        }
    }

    @Override
    public ResourceResponse moveResource(Long userId, String fromPath, String toPath) {
        Resource resource = ResourceFactory.create(userId, fromPath);

        String fullFrom = resource.fullPath();
        String fullTo = PathUtil.getFullPath(userId, toPath);

        if (!storageRepository.resourceExists(fullFrom)) {
            throw new ResourceNotFoundException("Source not found: " + fromPath);
        }
        if (storageRepository.resourceExists(fullTo)) {
            throw new ResourceAlreadyExistsException("Target already exists: " + toPath);
        }
        String parentFull = PathUtil.extractParentPath(fullTo);
        if (!parentFull.isEmpty() && !storageRepository.resourceExists(parentFull)) {
            throw new ResourceNotFoundException("Parent directory not found: " + parentFull);
        }

        if (resource.isDirectory()) {
            directoryService.move(fullFrom, fullTo);
        } else {
            fileService.move(fullFrom, fullTo);
        }

        return getInfoResource(userId, toPath);
    }

    @Override
    public List<ResourceResponse> searchResources(Long userId, String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        String normalizedQuery = query.toLowerCase();
        List<ResourceResponse> results = new ArrayList<>();

        String fullPrefix = PathUtil.getFullPath(userId, "");
        List<ResourceInfo> allObjects = storageRepository.listAllResourceRecursive(fullPrefix);

        for (ResourceInfo obj : allObjects) {
            String path = obj.path();
            String name = PathUtil.extractName(path);
            if (name.toLowerCase().contains(normalizedQuery)) {
                boolean isDirectory = path.endsWith("/");
                ResourceResponse response = ResourceResponse.builder()
                        .path(PathUtil.extractParentPath(path))
                        .name(name)
                        .size(isDirectory ? null : obj.size())
                        .type(isDirectory ? ResourceType.DIRECTORY : ResourceType.FILE)
                        .build();
                results.add(response);
            }
        }
        return results;
    }

    @Override
    public List<ResourceResponse> uploadFiles(Long userId, String path, List<MultipartFile> files) {
        String fullTargetPath = PathUtil.getFullPath(userId, path);

        if (!directoryService.exists(fullTargetPath)) {
            throw new ResourceNotFoundException("Target directory not found: " + path);
        }

        List<ResourceResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                throw new IllegalArgumentException("File name cannot be empty");
            }

            String fullFilePath = fullTargetPath + originalFilename;

            directoryService.ensureDirectoriesForFile(fullFilePath);

            if (storageRepository.resourceExists(fullFilePath)) {
                throw new ResourceAlreadyExistsException("File already exists: " + fullFilePath);
            }

            try {
                fileService.upload(fullFilePath, file.getInputStream(), file.getSize());
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file: " + originalFilename, e);
            }

            Resource resource = ResourceFactory.create(userId, fullFilePath);

            ResourceResponse response = resourceMapper.toResponseDto(resource, file.getSize());
            responses.add(response);
        }
        return responses;
    }
}