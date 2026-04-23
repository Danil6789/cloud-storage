package com.example.cloud_storage.controller.impl;


import com.example.cloud_storage.annotation.ValidPath;
import com.example.cloud_storage.controller.DirectoryApi;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.service.resource.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DirectoryController implements DirectoryApi {
    private final DirectoryService directoryService;

    @Override
    public ResponseEntity<List<ResourceResponse>> getDirectoryContents(@RequestParam @ValidPath String path) {
        List<ResourceResponse> contents = directoryService.getDirectoryContents(path);
        return ResponseEntity.ok(contents);
    }

    @Override
    public ResponseEntity<ResourceResponse> createDirectory(@RequestParam @ValidPath String path) {
        ResourceResponse created = directoryService.createDirectory(path);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}