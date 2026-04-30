package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.ResourceFactory;
import com.example.cloud_storage.dto.resource.ResourceInfo;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.exception.resource.BadRequestException;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.resource.S3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.example.cloud_storage.constant.ExceptionMessages.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final S3Repository s3Repository;
    private final PathService pathService;
    private final ResourceMapper resourceMapper;
    private final ResourceFactory resourceFactory;

    public List<ResourceResponse> search(String query) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException(BAD_REQUEST);
        }
        String normalizedQuery = query.toLowerCase();
        String fullPrefix = pathService.getCurrentUserRootPath();
        List<ResourceInfo> allObjects = s3Repository.listAllObjectsRecursive(fullPrefix);
        List<ResourceResponse> results = new ArrayList<>();

        for (ResourceInfo obj : allObjects) {
            String name = pathService.extractName(obj.path());
            if (name.toLowerCase().contains(normalizedQuery)) {
                Resource resource = resourceFactory.create(pathService.removeUserDirPrefix(obj.path()));
                ResourceResponse response = resourceMapper.toResponseDto(resource, obj.size());
                results.add(response);
            }
        }

        return results;
    }
}