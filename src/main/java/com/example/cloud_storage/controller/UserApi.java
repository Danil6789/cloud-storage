package com.example.cloud_storage.controller;

import com.example.cloud_storage.dto.UserDetailsImpl;
import com.example.cloud_storage.dto.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "User", description = "API для работы с пользователем")
@RequestMapping("/api/user")
public interface UserApi {

    @GetMapping("/me")
    @Operation(summary = "Получить информацию о текущем пользователе",
            description = "Возвращает username аутентифицированного пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация получена"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка")
    })
    ResponseEntity<UserResponse> getUser(@AuthenticationPrincipal UserDetailsImpl userDetails);
}
