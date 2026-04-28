package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.dto.resource.*;
import com.example.cloud_storage.dto.resource.response.DownloadResponse;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.exception.resource.ResourceAlreadyExistsException;
import com.example.cloud_storage.exception.resource.ResourceNotFoundException;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.S3Repository;
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
public class ResourceService {

    private final ResourceFactory resourceFactory; //TODO: Скорее всего оно не нужно. Надо подумать как убрать
    private final PathService pathService;
    private final S3Repository storageRepository; //TODO: Сделать так чтоб вызывалось только в fileService и directoryService
    private final ResourceMapper resourceMapper;
    private final FileService fileService; //TODO: Сделать чтоб указывался его интерфейс(надо создать его) чтоб соблюдался принцип DIP
    private final DirectoryService directoryService; //TODO: Сделать чтоб указывался его интерфейс(надо создать его) чтоб соблюдался принцип DIP

    public ResourceResponse getInfoResource(String path){
        Resource resource = resourceFactory.create(path);
        if(resource.isDirectory()){
            return directoryService.getInfo(resource);
        }
        return fileService.getInfo(resource);
    }

    public DownloadResponse downloadResource(String path) {
        Resource resource = resourceFactory.create(path);
        String name = resource.name();

        if (resource.isDirectory()) {
            StreamingResponseBody body = directoryService.download(resource.fullPath());
            return new DownloadResponse(body, name + ".zip", true);

        } else {
            StreamingResponseBody body = fileService.download(resource.fullPath());
            return new DownloadResponse(body, name, false);
        }
    }

    public void deleteResource(String path){
        Resource resource = resourceFactory.create(path);
        String fullPath = resource.fullPath();

        if (resource.isDirectory()) {
            directoryService.delete(fullPath);
        } else {
            fileService.delete(fullPath);
        }
    }


    public ResourceResponse moveOrRenameResource(String fromPath, String toPath) {
        Resource resourceFrom = resourceFactory.create(fromPath);
        Resource resourceTo = resourceFactory.create(toPath);

        String fullFrom = resourceFrom.fullPath();
        String fullTo = resourceTo.fullPath();

        if (!storageRepository.exists(fullFrom)) { //TODO: Определиться что вызывать DirectoryService или FileService
            throw new ResourceNotFoundException("Source not found: " + fromPath);
        }
        if (storageRepository.exists(fullTo)) {//TODO: Определиться что вызывать DirectoryService или FileService
            throw new ResourceAlreadyExistsException("Target already exists: " + toPath);
        }
        String parentToFull = pathService.extractParentPath(fullTo);
        if (!parentToFull.isEmpty() && !storageRepository.exists(parentToFull)) { //TODO: Определиться что вызывать DirectoryService или FileService
            throw new ResourceNotFoundException("Parent directory not found: " + parentToFull);
        }

        if (resourceFrom.isDirectory()) {
            directoryService.moveOrRename(fullFrom, fullTo);
        } else {
            fileService.moveOrRename(fullFrom, fullTo);
        }

        return getInfoResource(toPath);
    }

    public List<ResourceResponse> searchResources(String query) {//TODO: Класс Pattern как примере
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        String normalizedQuery = query.toLowerCase();
        List<ResourceResponse> results = new ArrayList<>();

        String fullPrefix = pathService.getCurrentUserRootPath();
        List<ResourceInfo> allObjects = storageRepository.listAllObjectsRecursive(fullPrefix); //TODO: удалить storageRespository

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

            Resource resource = resourceFactory.create(fullFilePath);

            ResourceResponse response = resourceMapper.toResponseDto(resource, file.getSize());
            responses.add(response);
        }

        return responses;
    }
}