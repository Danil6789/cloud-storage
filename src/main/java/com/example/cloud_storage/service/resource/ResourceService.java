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
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;


@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceFactory resourceFactory;
    private final PathService pathService;
    private final ResourceMapper resourceMapper;
    private final FileService fileService;
    private final DirectoryService directoryService;
    private final SearchService searchService;
    private final MoveOrRenameService moveOrRenameService;

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

    public List<ResourceResponse> searchResources(String query) {
        return searchService.search(query);
    }

    public ResourceResponse moveOrRenameResource(String fromPath, String toPath) {
        moveOrRenameService.moveOrRename(fromPath, toPath);
        return getInfoResource(toPath);
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

            directoryService.ensureDirectoriesForFile(fullFilePath);

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