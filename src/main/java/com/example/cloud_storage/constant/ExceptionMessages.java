package com.example.cloud_storage.constant;

public final class ExceptionMessages {

    // Auth
    public static final String UNAUTHORIZED = "Пользователь не авторизован";

    // Resource
    public static final String BAD_REQUEST = "Некорректный запрос";
    public static final String RESOURCE_ALREADY_EXISTS = "Ресурс уже существует";
    public static final String RESOURCE_NOT_FOUND = "Ресурс не найден";
    public static final String S3_OPERATION_ERROR = "Ошибка при операции с хранилищем";
    public static final String SERVER_IO_ERROR = "Внутренняя ошибка ввода-вывода";

    public static final String MOVE_OPERATION_EXCEPTION = "Ошибка перемещения ресурса";

    // User
    public static final String USER_ALREADY_EXISTS = "Пользователь с таким именем уже существует";
    public static final String USER_NOT_FOUND = "Пользователь не найден ";
}