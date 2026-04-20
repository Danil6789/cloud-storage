package com.example.cloud_storage.dto.resource.response;


import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;


public record DownloadResponse(
        StreamingResponseBody body,
    String fileName,
    boolean isDirectory
){}