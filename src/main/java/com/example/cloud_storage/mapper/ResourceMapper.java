package com.example.cloud_storage.mapper;

import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResourceMapper {

    default ResourceResponse toResponseDto(Resource resource, Long size){
        return new ResourceResponse(
                resource.parentPath(),
                resource.name(),
                size,
                resource.type()
        );
    }
}