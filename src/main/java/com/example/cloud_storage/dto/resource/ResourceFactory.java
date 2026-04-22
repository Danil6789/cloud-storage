package com.example.cloud_storage.dto.resource;

import com.example.cloud_storage.service.resource.PathService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.example.cloud_storage.dto.resource.ResourceType.DIRECTORY;
import static com.example.cloud_storage.dto.resource.ResourceType.FILE;

@Component
@RequiredArgsConstructor
public class ResourceFactory {
    private final PathService pathService;

    public Resource create(String path){
        String name = pathService.extractName(path);
        String parentPath = pathService.extractParentPath(path);
        String fullPath = pathService.getFullPath(path);
        //String relativePath = PathUtil.getRelativePath(fullPath, parentPath); //TODO: Пока ещё не совсем понятно для чего
        ResourceType type = path.endsWith("/") ? DIRECTORY : FILE;

        return new Resource(name, parentPath, fullPath, type);
    }
}
