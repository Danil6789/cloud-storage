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
import com.example.cloud_storage.service.resource.PathService;
import com.example.cloud_storage.service.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.cloud_storage.dto.resource.ResourceInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;


@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private final PathService pathService;
    private final S3Repository storageRepository; //TODO: Сделать так чтоб вызывалось только в fileService и directoryService
    private final ResourceMapper resourceMapper;
    private final FileService fileService; //TODO: Сделать чтоб указывался его интерфейс(надо создать его) чтоб соблюдался принцип DIP
    private final DirectoryService directoryService; //TODO: Сделать чтоб указывался его интерфейс(надо создать его) чтоб соблюдался принцип DIP

    @Override
    public ResourceResponse getInfoResource(String path){//TODO: Всё таки надо сделать чтоб было разделение на FileService и DirectoryService
        Resource resource = ResourceFactory.create(path);
        if(resource.isDirectory()){
            return directoryService.getInfoDirectory(resource);
        }
        return fileService.getInfo(resource);
    }

    @Override
    public DownloadResponse downloadResource(String path) {
        Resource resource = ResourceFactory.create(path);
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
    public void deleteResource(String path){
        Resource resource = ResourceFactory.create(path);
        if (resource.isDirectory()) {
            directoryService.delete(resource.fullPath());
        } else {
            fileService.delete(resource.fullPath());
        }
    }

    @Override
    public ResourceResponse moveResource(String fromPath, String toPath) {
        Resource resource = ResourceFactory.create(fromPath);

        String fullFrom = resource.fullPath();
        String fullTo = pathService.getFullPath(toPath);

        if (!storageRepository.resourceExists(fullFrom)) { //TODO: Определиться что вызывать DirectoryService или FileService
            throw new ResourceNotFoundException("Source not found: " + fromPath);
        }
        if (storageRepository.resourceExists(fullTo)) {//TODO: Определиться что вызывать DirectoryService или FileService
            throw new ResourceAlreadyExistsException("Target already exists: " + toPath);
        }
        String parentFull = pathService.extractParentPath(fullTo);
        if (!parentFull.isEmpty() && !storageRepository.resourceExists(parentFull)) { //TODO: Определиться что вызывать DirectoryService или FileService
            throw new ResourceNotFoundException("Parent directory not found: " + parentFull);
        }

        if (resource.isDirectory()) {
            directoryService.move(fullFrom, fullTo);
        } else {
            fileService.move(fullFrom, fullTo);
        }

        return getInfoResource(toPath);
    }

    @Override
    public List<ResourceResponse> searchResources(String query) {//TODO: Класс Pattern как примере
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        String normalizedQuery = query.toLowerCase();
        List<ResourceResponse> results = new ArrayList<>();

        String fullPrefix = pathService.getCurrentUserRootPath();
        List<ResourceInfo> allObjects = storageRepository.listAllResourceRecursive(fullPrefix); //TODO: удалить storageRespository

        for (ResourceInfo obj : allObjects) {
            String path = obj.path();
            String name = pathService.extractName(path);
            if (name.toLowerCase().contains(normalizedQuery)) {
                boolean isDirectory = path.endsWith("/");
                ResourceResponse response = ResourceResponse.builder()
                        .path(pathService.extractParentPath(path))
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
    public List<ResourceResponse> uploadFiles(String path, List<MultipartFile> files) {
        String fullPath = pathService.getFullPath(path);

        List<ResourceResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                throw new IllegalArgumentException("File name cannot be empty");
            }

            String fullFilePath = fullPath + originalFilename;

            directoryService.ensureDirectoriesForFile(fullFilePath); //TODO: для создания пустых папок в котором хранится этот файл

            if (fileService.exists(fullFilePath)) {
                throw new ResourceAlreadyExistsException("File already exists: " + fullFilePath);
            }

            fileService.upload(fullFilePath, file);

            Resource resource = ResourceFactory.create(fullFilePath);

            ResourceResponse response = resourceMapper.toResponseDto(resource, file.getSize());
            responses.add(response);
        }

        return responses;
    }
}