package com.example.cloud_storage.integration.resource;

import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.ResourceType;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.exception.resource.ResourceNotFoundException;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.resource.S3Repository;
import com.example.cloud_storage.service.resource.FileService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import java.io.ByteArrayOutputStream;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;



@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class FileServiceIntegrationTest {

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest")
            .withUserName("minioadmin")
            .withPassword("minioadmin123");

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.endpoint", minio::getS3URL);
        registry.add("minio.access-key", minio::getUserName);
        registry.add("minio.secret-key", minio::getPassword);
        registry.add("minio.bucket-name", () -> "user-files");
    }

    @Autowired
    private S3Repository s3Repository;

    @Autowired
    private FileService fileService;

    @Autowired
    private ResourceMapper resourceMapper;

    private final String bucketName = "user-files";
    private final String userPrefix = "user-1-files/";
    private final String testFileName = "test.txt";
    private final String fullPath = userPrefix + testFileName;
    private final byte[] testContent = "Hello, MinIO!".getBytes(StandardCharsets.UTF_8);
    private final String missingPath = userPrefix + "missing.txt";

    @BeforeEach
    void setUp() throws Exception {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minio.getS3URL())
                .credentials(minio.getUserName(), minio.getPassword())
                .build();
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @AfterEach
    void tearDown() {
        // Очищаем все объекты с префиксом пользователя после каждого теста
        List<String> objects = s3Repository.listDirectoryRecursive(userPrefix);
        for (String obj : objects) {
            s3Repository.delete(obj);
        }
    }

    private void uploadTestFile() {
        MultipartFile mockFile = new MockMultipartFile("file", testFileName, "text/plain", testContent);
        fileService.upload(fullPath, mockFile);
    }

    @Test
    void upload_shouldStoreFileInMinio() throws Exception {
        uploadTestFile();

        assertThat(s3Repository.exists(fullPath)).isTrue();
        try (InputStream is = s3Repository.downloadFile(fullPath)) {
            assertThat(is.readAllBytes()).isEqualTo(testContent);
        }
    }

    @Test
    void getInfo_shouldReturnResourceResponse_whenFileExists() throws Exception {
        uploadTestFile();

        Resource resource = new Resource(testFileName, userPrefix, fullPath, ResourceType.FILE);
        ResourceResponse response = fileService.getInfo(resource);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(testFileName);
        assertThat(response.size()).isEqualTo(testContent.length);
    }

    @Test
    void getInfo_shouldThrowResourceNotFoundException_whenFileNotExists() {
        Resource resource = new Resource("missing.txt", userPrefix, missingPath, ResourceType.FILE);
        assertThatThrownBy(() -> fileService.getInfo(resource))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void download_shouldReturnFileContent() throws Exception {
        uploadTestFile();

        StreamingResponseBody body = fileService.download(fullPath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        body.writeTo(baos);

        assertThat(baos.toByteArray()).isEqualTo(testContent);
    }

    @Test
    void exists_shouldReturnTrue_whenFileExists() throws Exception {
        uploadTestFile();
        assertThat(fileService.exists(fullPath)).isTrue();
    }

    @Test
    void exists_shouldReturnFalse_whenFileNotExists() {
        assertThat(fileService.exists(missingPath)).isFalse();
    }

    @Test
    void delete_shouldRemoveFileFromMinio() throws Exception {
        uploadTestFile();

        fileService.delete(fullPath);

        assertThat(s3Repository.exists(fullPath)).isFalse();
    }

    @Test
    void delete_shouldThrowResourceNotFoundException_whenFileNotExists() {
        assertThatThrownBy(() -> fileService.delete(missingPath))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void moveOrRename_shouldMoveFileToNewPath() throws Exception {
        uploadTestFile();
        String newPath = userPrefix + "moved.txt";

        fileService.moveOrRename(fullPath, newPath);

        assertThat(s3Repository.exists(fullPath)).isFalse();
        assertThat(s3Repository.exists(newPath)).isTrue();
        try (InputStream is = s3Repository.downloadFile(newPath)) {
            assertThat(is.readAllBytes()).isEqualTo(testContent);
        }
    }
}
