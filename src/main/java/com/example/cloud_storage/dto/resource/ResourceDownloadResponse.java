package com.example.cloud_storage.dto.resource;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class ResourceDownloadResponse {
    private InputStream inputStream;
    private String fileName;
    private boolean isDirectory;
    private long size;
}