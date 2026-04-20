package com.example.cloud_storage.dto.resource;

public record Resource(
        String name,
        //Long size,
        //String relativePath,
        String parentPath,
        String fullPath,
        ResourceType type
) {
    public boolean isDirectory(){
        return this.type() == ResourceType.DIRECTORY;
    }
}
