package com.example.cloud_storage.controller;

import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequestMapping("/api/directory")
public interface DirectoryApi {
    @GetMapping
    @Operation(summary = "Получить содержимое папки", description = "Возвращает список файлов и папок внутри указанной директории (не рекурсивно)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Содержимое папки получено"),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Папка не существует")
    })
    ResponseEntity<List<ResourceResponse>> getDirectoryContents(
            @RequestParam String path
    );

    @PostMapping
    @Operation(summary = "Создать пустую папку", description = "Создаёт новую директорию по указанному пути")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Папка успешно создана"),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь к новой папке"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Родительская папка не существует"),
            @ApiResponse(responseCode = "409", description = "Папка уже существует")
    })
    ResponseEntity<ResourceResponse> createDirectory(
            @RequestParam String path
    );
}