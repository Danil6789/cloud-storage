package com.example.cloud_storage.dto.resource;

import com.example.cloud_storage.util.PathUtil;
import lombok.RequiredArgsConstructor;

import static com.example.cloud_storage.dto.resource.ResourceType.DIRECTORY;
import static com.example.cloud_storage.dto.resource.ResourceType.FILE;

@RequiredArgsConstructor
public class ResourceFactory {

    public static Resource create(Long userId, String path){
        String name = PathUtil.extractName(path);
        String parentPath = PathUtil.extractParentPath(path);
        String fullPath = PathUtil.getFullPath(userId, path);
        //String relativePath = PathUtil.getRelativePath(fullPath, parentPath); //TODO: Пока ещё не совсем понятно для чего
        ResourceType type = path.endsWith("/") ? DIRECTORY : FILE;

        return new Resource(name, parentPath, fullPath, type);
    }
}
