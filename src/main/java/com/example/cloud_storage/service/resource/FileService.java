package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.repository.S3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileService {
    private final S3Repository s3Client;

    public StreamingResponseBody downloadStream(String path) {
        return outputStream -> {
            try (InputStream inputStream = s3Client.downloadFile(path)) {
                inputStream.transferTo(outputStream);
            }
        };
    }

    public void delete(String fullPath) {
        s3Client.deleteResource(fullPath);
    }

    public void upload(String fullPath, InputStream inputStream, long size) {
        s3Client.uploadFile(fullPath, inputStream, size);
    }

    public void move(String fullFromPath, String fullToPath) {
        s3Client.copyResource(fullFromPath, fullToPath);
        try{
            s3Client.deleteResource(fullFromPath);
        }catch(Exception e){
            s3Client.deleteResource(fullToPath);
            throw new RuntimeException("Move failed, rolled back", e);
        }
    }
}