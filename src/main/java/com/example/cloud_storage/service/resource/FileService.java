package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.S3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileService {
    private final S3Repository s3Repository;
    private final ResourceMapper resourceMapper;

    public ResourceResponse getInfo(Resource resource){ //TODO: Нужна ли тут проверка что в друг resource это папка???? Возможно  нужно использовать FileResource  DirectoryResource
        Long size = s3Repository.getFileSize(resource.fullPath());
        return resourceMapper.toResponseDto(resource, size);
    }

    public StreamingResponseBody downloadStream(String path) {
        return outputStream -> {
            try (InputStream inputStream = s3Repository.downloadFile(path)) {
                inputStream.transferTo(outputStream);
            }
        };
    }

    public void delete(String fullPath) {
        s3Repository.deleteResource(fullPath);
    }

    public void upload(String fullPath, InputStream inputStream, long size) {
        s3Repository.uploadFile(fullPath, inputStream, size);
    }

    public void move(String fullFromPath, String fullToPath) {
        s3Repository.copyResource(fullFromPath, fullToPath);
        try{
            s3Repository.deleteResource(fullFromPath);
        }catch(Exception e){
            s3Repository.deleteResource(fullToPath);
            throw new RuntimeException("Move failed, rolled back", e);
        }
    }
}