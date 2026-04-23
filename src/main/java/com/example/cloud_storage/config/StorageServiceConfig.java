package com.example.cloud_storage.config;


import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.S3Repository;
import com.example.cloud_storage.service.resource.DirectoryService;
import com.example.cloud_storage.service.resource.FileService;
import com.example.cloud_storage.service.resource.PathService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StorageServiceConfig {

    private final S3Repository s3Repository;
    private final ResourceMapper resourceMapper;
    private final PathService pathService;

    @Bean
    public FileService fileService() {
        return FileService.builder()
                .s3Repository(s3Repository)
                .resourceMapper(resourceMapper)
                .pathService(pathService)
                .build();
    }

    @Bean
    public DirectoryService directoryService() {
        return DirectoryService.builder()
                .s3Repository(s3Repository)
                .resourceMapper(resourceMapper)
                .pathService(pathService)
                .build();
    }
}