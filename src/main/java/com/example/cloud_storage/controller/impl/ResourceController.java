package com.example.cloud_storage.controller.impl;

import com.example.cloud_storage.controller.ResourceApi;
import com.example.cloud_storage.dto.UserDetailsImpl;
import com.example.cloud_storage.dto.resource.response.DownloadResponse;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.service.resource.ResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResourceController implements ResourceApi {
    private final ResourceService resourceService; //TODO: Зачем тут интерфейс если всегда будет только одна реализация этого сервиса

    @Override
    public ResponseEntity<ResourceResponse> getInfoResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        ResourceResponse response = resourceService.getInfoResource(userDetails.getId(), path);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> downloadResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        DownloadResponse response = resourceService.downloadResource(userDetails.getId(), path);
        String contentType = response.isDirectory() ? "application/zip" : "application/octet-stream";

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(response.fileName())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(response.body());
    }

    @Override
    public ResponseEntity<Void> deleteResource(
            @RequestParam String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        resourceService.deleteResource(userDetails.getId(), path);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ResourceResponse> moveResource(
            @RequestParam String from,
            @RequestParam String to,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        ResourceResponse response = resourceService.moveResource(userDetails.getId(), from, to);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<ResourceResponse>> searchResources(
            @RequestParam @NotBlank(message = "Search query cannot be empty") String query,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
//        if (query == null || query.isBlank()) {
//            throw new IllegalArgumentException("Search query cannot be empty");
//        }
        List<ResourceResponse> results = resourceService.searchResources(userDetails.getId(), query);
        return ResponseEntity.ok(results);
    }

    @Override
    public ResponseEntity<List<ResourceResponse>> uploadResources(
            @RequestParam String path,
            @RequestPart("files") MultipartFile[] files,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files to upload");
        }

        List<MultipartFile> fileList = Arrays.asList(files);
        List<ResourceResponse> uploaded = resourceService.uploadFiles(userDetails.getId(), path, fileList);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }
}