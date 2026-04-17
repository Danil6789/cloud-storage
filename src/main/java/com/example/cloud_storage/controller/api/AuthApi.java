package com.example.cloud_storage.controller.api;

import com.example.cloud_storage.dto.auth.SignInRequest;
import com.example.cloud_storage.dto.auth.SignResponse;
import com.example.cloud_storage.dto.auth.SignUpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static com.example.cloud_storage.constant.ApiPath.*;

@Tag(name = "Authentication", description = "API для аутентификации и регистрации")
public interface AuthApi {

    @PostMapping(AUTH_SIGN_UP_URL)
    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создаёт нового пользователя и автоматически выполняет вход. " +
                    "При успешной регистрации выставляется cookie сессии."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Username уже занят")
    })
    ResponseEntity<SignResponse> signUp(@RequestBody @Valid SignUpRequest signUpRequest);


    @PostMapping(AUTH_SIGN_IN_URL)
    @Operation(
            summary = "Вход в систему",
            description = "Аутентификация пользователя по username и password. При успешном входе выставляется cookie сессии."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации (пустые поля)"),
            @ApiResponse(responseCode = "401", description = "Неверные учётные данные")
    })
    ResponseEntity<SignResponse> signIn(@RequestBody @Valid SignInRequest signInRequest);

    @PostMapping(AUTH_SIGN_OUT_URL)
    @Operation(summary = "Выход из системы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Успешный выход"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    ResponseEntity<Void> signOut();
}