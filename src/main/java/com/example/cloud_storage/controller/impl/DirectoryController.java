package com.example.cloud_storage.controller.impl;


import com.example.cloud_storage.controller.DirectoryApi;
import com.example.cloud_storage.dto.UserDetailsImpl;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.service.resource.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DirectoryController implements DirectoryApi {
    private final DirectoryService directoryService;

    @Override
    public ResponseEntity<List<ResourceResponse>> getDirectoryContents(String path, UserDetailsImpl userDetails) {
        List<ResourceResponse> contents = directoryService.getDirectoryContents(userDetails.getId(), path);
        return ResponseEntity.ok(contents);
    }

    @Override
    public ResponseEntity<ResourceResponse> createDirectory(String path, UserDetailsImpl userDetails) {
        ResourceResponse created = directoryService.createDirectory(userDetails.getId(), path);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
