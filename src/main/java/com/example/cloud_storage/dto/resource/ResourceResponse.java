package com.example.cloud_storage.dto.resource;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceResponse {
    private String path;
    private String name;
    private Long size;
    private ResourceType type;
}