# Первый этап: сборка (build)
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# Копируем только файлы сборки для кэширования зависимостей
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./

# Даём права на выполнение и скачиваем зависимости (кешируется)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Копируем исходный код и собираем JAR (пропускаем тесты для ускорения)
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# Второй этап: запуск (runtime)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем собранный JAR из предыдущего этапа
COPY --from=builder /app/build/libs/*.jar app.jar

# Профиль Spring Boot по умолчанию – prod
ENV SPRING_PROFILES_ACTIVE=prod

# Порт приложения
EXPOSE 8080

# Запуск
ENTRYPOINT ["java", "-jar", "app.jar"]