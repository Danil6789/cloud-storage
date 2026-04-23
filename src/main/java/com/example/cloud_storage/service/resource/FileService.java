package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.exception.resource.ResourceNotFoundException;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.S3Repository;
import com.example.cloud_storage.service.BaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

@Slf4j
@Service
@SuperBuilder
public class FileService extends BaseStorageService {
    private final PathService pathService; //TODO: пока не нужно это поле???? Если так то удалить
    private final S3Repository s3Repository;
    private final ResourceMapper resourceMapper;

    @Override
    public ResourceResponse getInfo(Resource resource){
        if(!exists(resource.fullPath())){
            throw new ResourceNotFoundException("Такого файла нет");
        }

        Long size = s3Repository.getFileSize(resource.fullPath());
        return resourceMapper.toResponseDto(resource, size);
    }

    @Override
    public StreamingResponseBody download(String path) {

        return outputStream -> {
            try (InputStream inputStream = s3Repository.downloadFile(path)) {
                inputStream.transferTo(outputStream);
            }
        };
    }

    @Override
    public void delete(String fullPath) {
        if(!exists(fullPath)){
            throw new ResourceNotFoundException("Такой файл не найден");
        }
        s3Repository.delete(fullPath);
    }

    public void upload(String fullPath, MultipartFile file) {
        s3Repository.uploadFile(fullPath, file);
    }

    @Override
    public void moveOrRename(String fullFromPath, String fullToPath) {
        s3Repository.copy(fullFromPath, fullToPath);
        try{
            s3Repository.delete(fullFromPath);
        }catch(Exception e){
            s3Repository.delete(fullToPath);
            throw new RuntimeException("Move failed, rolled back", e);
        }
    }
}