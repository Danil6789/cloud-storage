package com.example.cloud_storage.dto.resource.response;

import com.example.cloud_storage.dto.resource.ResourceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ResourceResponse(String path, String  name, Long size, ResourceType type) {}