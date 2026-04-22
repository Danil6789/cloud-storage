package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.dto.resource.response.DownloadResponse;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResourceService {
    ResourceResponse getInfoResource(String path);

    DownloadResponse downloadResource(String path);

    void deleteResource(String path);

    ResourceResponse moveResource(String fromPath, String toPath);

    List<ResourceResponse> searchResources(String query);

    List<ResourceResponse> uploadFiles(String path, List<MultipartFile> files);
}