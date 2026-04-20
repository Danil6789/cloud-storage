package com.example.cloud_storage.service;

import com.example.cloud_storage.dto.resource.response.DownloadResponse;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResourceService {
    ResourceResponse getInfoResource(Long userId, String path);

    DownloadResponse downloadResource(Long userId, String path);

    void deleteResource(Long userId, String path);

    ResourceResponse moveResource(Long userId, String fromPath, String toPath);

    List<ResourceResponse> searchResources(Long userId, String query);

    List<ResourceResponse> uploadFiles(Long userId, String path, List<MultipartFile> files);
}