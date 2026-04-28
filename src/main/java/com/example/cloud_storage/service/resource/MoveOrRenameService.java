package com.example.cloud_storage.service.resource;

import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.ResourceFactory;
import com.example.cloud_storage.exception.resource.ResourceAlreadyExistsException;
import com.example.cloud_storage.exception.resource.ResourceNotFoundException;
import com.example.cloud_storage.repository.S3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoveOrRenameService {
    private final ResourceFactory resourceFactory;
    private final PathService pathService;
    private final S3Repository s3Repository;
    private final DirectoryService directoryService;
    private final FileService fileService;

    public void moveOrRename(String fromPath, String toPath) {
        Resource resourceFrom = resourceFactory.create(fromPath);
        Resource resourceTo = resourceFactory.create(toPath);

        String fullFrom = resourceFrom.fullPath();
        String fullTo = resourceTo.fullPath();

        if (!s3Repository.exists(fullFrom)) {
            throw new ResourceNotFoundException("Source not found: " + fromPath);
        }
        if (s3Repository.exists(fullTo)) {
            throw new ResourceAlreadyExistsException("Target already exists: " + toPath);
        }
        String parentToFull = pathService.extractParentPath(fullTo);
        if (!parentToFull.isEmpty() && !s3Repository.exists(parentToFull)) {
            throw new ResourceNotFoundException("Parent directory not found: " + parentToFull);
        }

        if (resourceFrom.isDirectory()) {
            directoryService.moveOrRename(fullFrom, fullTo);
        } else {
            fileService.moveOrRename(fullFrom, fullTo);
        }
    }
}