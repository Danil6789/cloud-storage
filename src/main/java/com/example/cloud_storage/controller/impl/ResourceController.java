package com.example.cloud_storage.controller.impl;

import com.example.cloud_storage.annotation.ValidPath;
import com.example.cloud_storage.controller.ResourceApi;
import com.example.cloud_storage.dto.resource.response.DownloadResponse;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.service.resource.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class ResourceController implements ResourceApi {
    private final ResourceService resourceService;

    @Override
    public ResponseEntity<ResourceResponse> getInfoResource(
            @RequestParam @ValidPath String path
    ){
        ResourceResponse response = resourceService.getInfoResource(path);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> downloadResource(
            @RequestParam @ValidPath String path
    ){
        DownloadResponse response = resourceService.downloadResource(path);
        String contentType = response.isDirectory() ? "application/zip" : "application/octet-stream";

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(response.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(response.body());
    }

    @Override
    public ResponseEntity<Void> deleteResource(
            @RequestParam @ValidPath String path
    ){
        resourceService.deleteResource(path);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ResourceResponse> moveResource(
            @RequestParam @ValidPath String from,
            @RequestParam @ValidPath String to
    ){
        ResourceResponse response = resourceService.moveOrRenameResource(from, to);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<ResourceResponse>> searchResources(
            @RequestParam @ValidPath String query
    ){

        List<ResourceResponse> results = resourceService.searchResources(query);
        return ResponseEntity.ok(results);
    }

    @Override
    public ResponseEntity<List<ResourceResponse>> uploadResources(
            @RequestParam @ValidPath String path,
            @RequestPart("files") MultipartFile[] files) {

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files to upload");
        }

        List<MultipartFile> fileList = Arrays.asList(files);
        List<ResourceResponse> uploaded = resourceService.uploadFiles(path, fileList);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }
}