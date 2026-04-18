package com.example.cloud_storage.controller.impl;

import com.example.cloud_storage.controller.api.ResourceApi;
import com.example.cloud_storage.dto.UserDetailsImpl;
import com.example.cloud_storage.dto.resource.ResourceDownloadResponse;
import com.example.cloud_storage.dto.resource.ResourceResponse;
import com.example.cloud_storage.service.ResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/resource")
public class ResourceController implements ResourceApi {
    private final ResourceService resourceService;

    @Override
    public ResponseEntity<ResourceResponse> getInfoResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        Long userId = userDetails.getId();
        ResourceResponse response = resourceService.getInfoResource(userId, path);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<InputStreamResource> downloadResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        Long userId = userDetails.getId();
        ResourceDownloadResponse downloadResponse = resourceService.downloadResource(userId, path);

        String contentType = determineContentType(downloadResponse);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, //TODO: Для создания заголовка можно воспользоватсья классом ContentDisposition
                        "attachment; filename=\"" + downloadResponse.getFileName() + "\"")
                .body(new InputStreamResource(downloadResponse.getInputStream()));
    }

    @Override
    public ResponseEntity<Void> deleteResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        Long userId = userDetails.getId();
        resourceService.deleteResource(userId, path);
        return ResponseEntity.noContent().build();
    }


    private String determineContentType(ResourceDownloadResponse downloadResponse) {
        if (downloadResponse.isDirectory()) {
            return "application/zip";
        }
        return "application/octet-stream";
    }

    @Override
    public ResponseEntity<ResourceResponse> moveResource(
            @RequestParam String from,
            @RequestParam String to,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        Long userId = userDetails.getId();
        ResourceResponse response = resourceService.moveResource(userId, from, to);
        return ResponseEntity.ok(response);
    }


    @Override
    public ResponseEntity<List<ResourceResponse>> searchResources(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        Long userId = userDetails.getId();
        List<ResourceResponse> results = resourceService.searchResources(userId, query);
        return ResponseEntity.ok(results);
    }

    @Override
    public ResponseEntity<List<ResourceResponse>> uploadResources(
            @RequestParam String path,
            @Parameter(description = "Файлы для загрузки",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            MultipartFile[] files,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getId();

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files to upload");
        }

        List<MultipartFile> fileList = Arrays.asList(files);
        List<ResourceResponse> uploaded = resourceService.uploadFiles(userId, path, fileList);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }
}