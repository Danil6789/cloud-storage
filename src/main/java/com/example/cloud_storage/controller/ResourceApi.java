package com.example.cloud_storage.controller;

import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import static com.example.cloud_storage.constant.ApiPath.*;

import java.util.List;

@Tag(name = "Resources", description = "API для работы с файлами и папками")
@RequestMapping("/api/resource")
public interface ResourceApi {

    @GetMapping
    @Operation(summary = "Получить информацию о ресурсе", description = "Возвращает информацию о файле или папке")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация получена"),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден")
    })
    ResponseEntity<ResourceResponse> getInfoResource(
            @RequestParam String path
    );

    @GetMapping(DOWNLOAD_RESOURCE)
    @Operation(summary = "Скачать ресурс", description = "Скачивает файл или папку (папка скачивается в формате ZIP)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл успешно скачан"),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден")
    })
    ResponseEntity<StreamingResponseBody> downloadResource(
            @RequestParam String path
    );


    @DeleteMapping
    @Operation(summary = "Удалить ресурс", description = "Удаляет файл или папку (папка удаляется рекурсивно)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ресурс успешно удалён"),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден")
    })
    ResponseEntity<Void> deleteResource(
            @RequestParam String path
    );


    @GetMapping(MOVE_RESOURCE)
    @Operation(summary = "Переместить/переименовать ресурс", description = "Перемещает или переименовывает файл/папку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ресурс перемещён"),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Исходный ресурс не найден"),
            @ApiResponse(responseCode = "409", description = "Целевой ресурс уже существует")
    })
    ResponseEntity<ResourceResponse> moveResource(
            @RequestParam String from,
            @RequestParam String to
    );

    @GetMapping(FIND_RESOURCE)
    @Operation(summary = "Поиск ресурсов", description = "Поиск файлов по имени (без учёта регистра)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Результаты поиска"),
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий поисковый запрос"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован")
    })
    ResponseEntity<List<ResourceResponse>> searchResources(
            @RequestParam String query
    );

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить файл(ы)", description = "Загружает один или несколько файлов в указанную папку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Файлы успешно загружены"),
            @ApiResponse(responseCode = "400", description = "Невалидное тело запроса или путь"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "404", description = "Целевая папка не найдена"),
            @ApiResponse(responseCode = "409", description = "Файл уже существует")
    })
    ResponseEntity<List<ResourceResponse>> uploadResources(
            @RequestParam String path,
            @RequestPart("files")
            @Parameter(description = "Файлы для загрузки",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            MultipartFile[] files
    );
}