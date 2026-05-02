package com.example.cloud_storage.integration.resource;

import com.example.cloud_storage.dto.user.UserDetailsImpl;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.exception.resource.BadRequestException;
import com.example.cloud_storage.repository.resource.S3Repository;
import com.example.cloud_storage.service.resource.FileService;
import com.example.cloud_storage.service.resource.DirectoryService;
import com.example.cloud_storage.service.resource.SearchService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
public class SearchServiceIntegrationTest {

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
    private DirectoryService directoryService;
    @Autowired
    private SearchService searchService;

    private final Long userId = 1L;
    private final String userRoot = "user-1-files/";
    private final byte[] content = "content".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUpSecurityContext() {
        UserDetailsImpl userDetails = new UserDetailsImpl(userId, "testuser", "password");
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUpBucket() throws Exception {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minio.getS3URL())
                .credentials(minio.getUserName(), minio.getPassword())
                .build();
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket("user-files").build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket("user-files").build());
        }
        if (!s3Repository.exists(userRoot)) {
            s3Repository.createDirectory(userRoot);
        }
    }

    @AfterEach
    void tearDown() {
        List<String> objects = s3Repository.listDirectoryRecursive(userRoot);
        for (String obj : objects) {
            s3Repository.delete(obj);
        }
    }

    private void uploadFile(String relativePath, String name) {
        MultipartFile file = new MockMultipartFile("file", name, "text/plain", content);
        String fullPath = userRoot + relativePath + name;
        fileService.upload(fullPath, file);
    }

    private void createFolder(String relativePath) {
        directoryService.createDirectory(userRoot + relativePath);
    }

    @Test
    void search_shouldFindFilesByName() {
        uploadFile("", "readme.txt");
        uploadFile("", "report.pdf");
        List<ResourceResponse> results = searchService.search("report");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("report.pdf");
    }

    @Test
    void search_shouldReturnEmpty_whenNoMatch() {
        uploadFile("", "data.csv");
        assertThat(searchService.search("nothing")).isEmpty();
    }

    @Test
    void search_shouldBeCaseInsensitive() {
        uploadFile("", "MiXedCase.txt");
        assertThat(searchService.search("mixedcase")).hasSize(1);
    }

    @Test
    void search_shouldFindFilesInSubdirectories() {
        createFolder("docs/");
        uploadFile("docs/", "manual.pdf");
        List<ResourceResponse> results = searchService.search("manual");
        assertThat(results).hasSize(1);
    }

    @Test
    void search_shouldThrowBadRequest_whenQueryEmpty() {
        assertThatThrownBy(() -> searchService.search(""))
                .isInstanceOf(BadRequestException.class);
    }
}