package com.example.cloud_storage.dto.resource;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileInfo {
    private String path;
    private Long size;
}
