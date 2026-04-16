package com.example.cloud_storage.dto.resource;

import lombok.Data;

@Data
public class ResourceDto {
    private String path;
    private String name;
    private Long size;
    private ResourceType type;
}