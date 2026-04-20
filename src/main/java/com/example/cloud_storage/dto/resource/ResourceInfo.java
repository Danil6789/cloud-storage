package com.example.cloud_storage.dto.resource;

import lombok.Builder;

@Builder
public record ResourceInfo(String path, Long size) {}
