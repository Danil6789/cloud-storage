package com.example.cloud_storage.controller;

import com.example.cloud_storage.dto.UserDetailsImpl;
import com.example.cloud_storage.dto.resource.ResourceDownloadResponse;
import com.example.cloud_storage.dto.resource.ResourceResponse;
import com.example.cloud_storage.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/resource")
public class ResourceController {
    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<ResourceResponse> getInfoResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        Long userId = userDetails.getId();
        ResourceResponse response = resourceService.getInfoResource(userId, path);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        Long userId = userDetails.getId();
        ResourceDownloadResponse downloadResponse = resourceService.downloadResource(userId, path);

        String contentType = determineContentType(downloadResponse);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadResponse.getFileName() + "\"")
                .body(new InputStreamResource(downloadResponse.getInputStream()));
    }

    @DeleteMapping
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

    @GetMapping("/move")
    public ResponseEntity<ResourceResponse> moveResource(
            @RequestParam String from,
            @RequestParam String to,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getId();
        ResourceResponse response = resourceService.moveResource(userId, from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceResponse>> searchResources(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        Long userId = userDetails.getId();
        List<ResourceResponse> results = resourceService.searchResources(userId, query);
        return ResponseEntity.ok(results);
    }

    @PostMapping
    public ResponseEntity<List<ResourceResponse>> uploadResources(
            @RequestParam String path,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getId();

        // Проверка на пустой список файлов (400)
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files to upload");
        }

        List<ResourceResponse> uploaded = resourceService.uploadFiles(userId, path, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }
}