package com.example.cloud_storage.service;

import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.S3Repository;
import com.example.cloud_storage.service.resource.PathService;
import lombok.experimental.SuperBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@SuperBuilder
public abstract class BaseStorageService {

    protected final S3Repository s3Repository;
    protected final ResourceMapper resourceMapper;
    protected final PathService pathService;

    protected BaseStorageService(S3Repository s3Repository, ResourceMapper resourceMapper, PathService pathService) {
        this.s3Repository = s3Repository;
        this.resourceMapper = resourceMapper;
        this.pathService = pathService;
    }


    public boolean exists(String fullPath) {
        return s3Repository.exists(fullPath);
    }

    public abstract void moveOrRename(String fullFromPath, String fullToPath);

    public abstract void delete(String fullPath);

    public abstract ResourceResponse getInfo(Resource resource);

    public abstract StreamingResponseBody download(String path);

    //public abstract upload() //TODO: сделать upload в DirectoryService
}
