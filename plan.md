# Cloud Storage (Google Drive Clone) - План разработки

## Общая концепция
- **Backend:** Spring Boot (MVC, Security, Data JPA), MinIO (S3), Redis (Sessions)
- **Frontend:** React (статика встроена в Spring Boot)
- **Хранение:**
    - PostgreSQL: пользователи
    - MinIO: файлы и папки
    - Redis: сессии
- **DevOps:** Docker Compose (Postgres, MinIO, Redis), деплой на VPS

---

## 🎯 Milestone 1: Базовая инфраструктура (Docker + DB + Auth)

### 1.1 Инициализация проекта
- [✅] Создать Spring Boot проект через https://start.spring.io/
- [✅] Выбрать зависимости:
    - Spring Web
    - Spring Security
    - Spring Data JPA
    - Spring Session (Redis)
    - PostgreSQL Driver
    - Lombok
    - Validation
    - Testcontainers
    - Spring Boot DevTools
- [✅] Настроить `application.yml` (профили dev, test, prod)
- [✅] Настроить систему сборки (Maven/Gradle)

### 1.2 Docker Compose (локальная инфраструктура)
- [ ] Создать `docker-compose.yml` в корне проекта
- [ ] Добавить сервисы:
    - **PostgreSQL** (порт 5432)
    - **MinIO** (порты 9000, 9001)
    - **Redis** (порт 6379)
- [ ] Настроить volumes для сохранения данных
- [ ] Создать `.env` файл с паролями (не в коммит)
- [ ] Проверить запуск: `docker-compose up -d`

### 1.3 Модель User + Spring Security
- [✅] Создать Entity `User`:
    - `id` (Long)
    - `username` (уникальный)
    - `password` (хеш)
- [✅] Создать `UserRepository` (Spring Data JPA)
- [✅] Создать `UserDetailsServiceImpl` (интеграция с Spring Security)
- [✅] Настроить `SecurityConfig`:
    - `BCryptPasswordEncoder`
    - Цепочка фильтров (пока отключить CSRF, разрешить `/api/auth/**`)
    - Сессия: `SessionCreationPolicy.IF_REQUIRED` (Spring Session будет управлять)

### 1.4 Регистрация и авторизация (RPC API)
- [✅] Создать пакет `controller.api.auth`
- [✅] Создать DTO: `SignUpRequest`, `SignInRequest`
- [✅] Создать `AuthController`:
    - `POST /api/auth/sign-up` → регистрация + сразу создать сессию
    - `POST /api/auth/sign-in` → аутентификация
    - `POST /api/auth/sign-out` → логаут (инвалидация сессии)
- [✅] Подключить `AuthenticationManager` для sign-in
- [✅] Настроить исключения и `@ControllerAdvice` (формат ошибки из ТЗ)

### 1.5 Интеграционные тесты Auth
- [✅] Настроить Testcontainers (PostgreSQL)
- [✅] Написать тесты:
    - Успешная регистрация → запись в БД
    - Регистрация с существующим username → `409 Conflict`
    - Успешный вход → `200 OK` + кука сессии
    - Неверный пароль → `401 Unauthorized`

---

## 🎯 Milestone 2: Файловое хранилище (MinIO + S3 API)

### 2.1 Интеграция MinIO SDK
- [✅] Добавить зависимость `minio` в `build.gradle/pom.xml`
- [✅] Создать `MinioConfig` (бин `MinioClient`, чтение endpoint/accessKey/secretKey)
- [✅] Создать `MinioProperties` для маппинга настроек из `application.yml`
- [✅] Создать бакет `user-files` при старте приложения (если не существует)

### 2.2 Сервис для работы с S3
- [ ] Создать `FileStorageService` (интерфейс)
- [ ] Реализация `MinioFileStorageService`:
    - `uploadFile(userId, path, InputStream)` → сохранить в `user-{userId}-files/{path}`
    - `downloadFile(userId, path)` → получить `InputStream`
    - `deleteResource(userId, path)` → удалить объект
    - `moveResource(userId, fromPath, toPath)` → скопировать + удалить
    - `listDirectory(userId, path)` → список объектов с префиксом
- [ ] Обратить внимание: в S3 нет папок, использовать "префиксы" и `/` в имени

### 2.3 Модели для работы с ресурсами
- [ ] Создать `ResourceType` (FILE, DIRECTORY)
- [ ] Создать `ResourceDto` (path, name, size, type)
- [ ] Создать кастомные исключения:
    - `ResourceNotFoundException`
    - `ResourceAlreadyExistsException`
    - `S3OperationException`

### 2.4 Базовый REST API для файлов
- [ ] Создать `ResourceController`:
    - `GET /api/resource?path=` → информация о ресурсе
    - `DELETE /api/resource?path=` → удаление
    - `GET /api/resource/download?path=` → скачивание (stream)
    - `GET /api/resource/move?from=&to=` → перемещение/переименование
- [ ] Временно добавить заглушки для папок (отдавать пустой список)

---

## 🎯 Milestone 3: Работа с папками и загрузка файлов

### 3.1 Сервис для работы с иерархией (папки)
- [ ] Создать `DirectoryService` (бизнес-логика поверх `FileStorageService`):
    - `createEmptyFolder(userId, path)` → создать "пустой маркер" (например, папка как объект с `/` в конце)
    - `getDirectoryContent(userId, path)` → список ресурсов внутри
    - `validatePath(userId, path)` → проверка существования родительской папки
- [ ] Реализовать логику: папка — это специальный объект нулевого размера с именем, заканчивающимся на `/`

### 3.2 API для папок
- [ ] Создать `DirectoryController`:
    - `GET /api/directory?path=` → содержимое папки
    - `POST /api/directory?path=` → создать пустую папку
- [ ] Проверить: путь к папке должен заканчиваться на `/`

### 3.3 Загрузка файлов (Upload)
- [ ] Реализовать `POST /api/resource?path=` (Multipart)
- [ ] Поддержка загрузки:
    - Одиночного файла
    - Вложенных папок (из `MultipartFile.getOriginalFilename()` извлекать относительный путь)
- [ ] Логика: `path` в запросе — это целевая папка. Добавлять к ней структуру из имени файла.
- [ ] Вернуть список загруженных ресурсов (`201 Created`)

### 3.4 Скачивание папки как ZIP
- [ ] Модифицировать `GET /api/resource/download`:
    - Если ресурс — папка → рекурсивно собрать все файлы → заархивировать в `ZipOutputStream`
    - Установить `Content-Type: application/zip`
- [ ] Внимание: не загружать всё в память, использовать streaming

---

## 🎯 Milestone 4: Поиск и фронтенд

### 4.1 Поиск файлов
- [ ] Реализовать `SearchService`:
    - Пройти по всем объектам пользователя в MinIO
    - Фильтровать по имени (содержит подстроку query)
    - MinIO API позволяет листинг с префиксом и рекурсивно
- [ ] Оптимизация: пока без индексов, просто `listObjects` с рекурсивным флагом
- [ ] Создать `GET /api/resource/search?query=`

### 4.2 Интеграция React фронтенда
- [ ] Скачать собранный фронтенд (папка `dist`) из репозитория
- [ ] Скопировать содержимое `dist` в `src/main/resources/static`
- [ ] Настроить Spring Boot:
    - Отдавать `index.html` на `GET /`
    - Все не-`/api` запросы → frontend routing
- [ ] Проверить: фронтенд обращается к `/api/*`, бэкенд отвечает

### 4.3 CORS и настройки для фронта
- [ ] Настроить CORS в Spring Security (разрешить с того же origin)
- [ ] Убедиться, что кука сессии отправляется (SameSite, Secure если HTTPS)

---

## 🎯 Milestone 5: Сессии в Redis + полировка

### 5.1 Настройка Spring Session с Redis
- [ ] Добавить зависимость `spring-session-data-redis`
- [ ] Добавить конфигурацию Redis в `application.yml` (хост, порт)
- [ ] Настроить `@EnableRedisHttpSession`:
    - `maxInactiveIntervalInSeconds` (например, 86400 = 1 день)
- [ ] Проверить: после логина в Redis появляется сессия
- [ ] Убедиться, что кука `SESSION` (или переименовать в `SESSION_ID`)

### 5.2 Интеграционные тесты с Testcontainers
- [ ] Добавить контейнеры: PostgreSQL + MinIO + Redis
- [ ] Написать тесты для `DirectoryService`:
    - Создание папки → объект появился в MinIO
    - Загрузка файла → файл доступен для скачивания
    - Перемещение → старый путь исчез, новый появился
- [ ] Написать тесты для изоляции пользователей (пользователь A не видит файлы B)

### 5.3 Swagger документация
- [ ] Добавить `springdoc-openapi-starter-webmvc-ui`
- [ ] Аннотировать контроллеры: `@Operation`, `@ApiResponse`
- [ ] Проверить `http://localhost:8080/swagger-ui.html`

---

## 🎯 Milestone 6: Деплой на VPS

### 6.1 Подготовка к продакшну
- [ ] Создать `application-prod.yml`:
    - Реальные URL для Postgres, Redis, MinIO
    - JWT-секреты, пароли через переменные окружения
- [ ] Собрать JAR: `mvn clean package` или `gradle bootJar`
- [ ] Создать `Dockerfile` для приложения (multi-stage или просто JAR + JRE)

### 6.2 Настройка сервера
- [ ] Арендовать VPS (DigitalOcean, Hetzner, Timeweb)
- [ ] Установить Docker и Docker Compose
- [ ] Скопировать на сервер:
    - `docker-compose.yml` (только Postgres, MinIO, Redis — без приложения)
    - `docker-compose.app.yml` (для самого приложения)
    - собранный JAR

### 6.3 Запуск на сервере
- [ ] Запустить инфраструктуру: `docker-compose up -d`
- [ ] Запустить приложение:
    - Через `java -jar app.jar --spring.profiles.active=prod`
    - Или через Docker Compose с собственным образом
- [ ] Настроить Nginx (опционально) для проброса 80 → 8080
- [ ] Проверить: `http://<server_ip>:8080` — работает фронтенд

---

## 🎓 Что вы изучите в итоге

- ✅ Spring Boot без магии XML
- ✅ Spring Security + Spring Session + Redis
- ✅ Работа с S3-совместимыми хранилищами (MinIO)
- ✅ Проектирование REST API по ТЗ
- ✅ Интеграция React SPA в Spring Boot
- ✅ Docker Compose для окружения разработки
- ✅ Testcontainers для интеграционных тестов
- ✅ Деплой на VPS с ручным управлением

---

## 📌 Рекомендуемый порядок на каждый день

1. **День 1-2:** Milestone 1 (Docker + Auth)
2. **День 3-4:** Milestone 2 (MinIO + базовые операции)
3. **День 5-6:** Milestone 3 (папки, upload, download zip)
4. **День 7-8:** Milestone 4 (поиск + фронтенд)
5. **День 9:** Milestone 5 (Redis сессии + тесты)
6. **День 10:** Milestone 6 (деплой + документация)

**Совет:** Начинайте каждый день с запуска `docker-compose up`, чтобы Postgres, MinIO и Redis были живы. И пишите по одному интеграционному тесту на каждый новый сервис.