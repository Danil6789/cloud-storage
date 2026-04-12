package com.example.cloud_storage.mapper;

import com.example.cloud_storage.dto.auth.SignResponse;
import com.example.cloud_storage.dto.auth.SignUpRequest;
import com.example.cloud_storage.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toEntity(SignUpRequest signUpRequest);

    SignResponse toDtoResponse(User user);
}