package com.example.cloud_storage.integration.resource;

import com.example.cloud_storage.dto.user.UserDetailsImpl;
import com.example.cloud_storage.dto.resource.Resource;
import com.example.cloud_storage.dto.resource.ResourceFactory;
import com.example.cloud_storage.dto.resource.response.ResourceResponse;
import com.example.cloud_storage.exception.resource.BadRequestException;
import com.example.cloud_storage.exception.resource.ResourceAlreadyExistsException;
import com.example.cloud_storage.exception.resource.ResourceNotFoundException;
import com.example.cloud_storage.mapper.ResourceMapper;
import com.example.cloud_storage.repository.resource.S3Repository;
import com.example.cloud_storage.service.resource.DirectoryService;
import com.example.cloud_storage.service.resource.FileService;
import com.example.cloud_storage.service.resource.PathService;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
public class DirectoryServiceIntegrationTest {

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
    private PathService pathService;

    @Autowired
    private ResourceFactory resourceFactory;

    @Autowired
    private ResourceMapper resourceMapper;

    private final String bucketName = "user-files";
    private final Long userId = 1L;
    private final String userRoot = "user-1-files/";
    private final String testDir = "test-dir/";
    private final String testDirPath = userRoot + testDir;
    private final String testFileName = "file.txt";
    private final byte[] testContent = "Hello, MinIO!".getBytes(StandardCharsets.UTF_8);

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
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
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

    private void uploadTestFile(String relativePath) {
        MultipartFile mockFile = new MockMultipartFile("file", testFileName, "text/plain", testContent);
        String fullPath = userRoot + relativePath;
        fileService.upload(fullPath, mockFile);
    }

    private void createTestDirectory(String relativePath) {
        directoryService.createDirectory(relativePath);
    }

    @Test
    void createDirectory_shouldCreateNewDirectory() {
        String dirName = "new-dir/";
        ResourceResponse response = directoryService.createDirectory(dirName);
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(dirName);
        assertThat(s3Repository.exists(userRoot + dirName)).isTrue();
    }

    @Test
    void createDirectory_shouldThrowException_whenDirectoryAlreadyExists() {
        String dirName = "existing-dir/";
        directoryService.createDirectory(dirName);
        assertThatThrownBy(() -> directoryService.createDirectory(dirName))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createUserDirectory_shouldCreateRootDirectoryForUser() {
        Long newUserId = 999L;
        String expectedRoot = "user-999-files/";
        directoryService.createUserDirectory(newUserId);
        assertThat(s3Repository.exists(expectedRoot)).isTrue();
    }

    @Test
    void createUserDirectory_shouldThrowException_whenRootAlreadyExists() {
        assertThatThrownBy(() -> directoryService.createUserDirectory(userId))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void getInfo_shouldReturnDirectoryInfo_whenDirectoryExists() {
        createTestDirectory(testDir);
        Resource resource = resourceFactory.create(testDir);
        ResourceResponse response = directoryService.getInfo(resource);
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(testDir);
        assertThat(response.size()).isNull();
    }

    @Test
    void getInfo_shouldThrowException_whenDirectoryDoesNotExist() {
        String nonExistentDir = "non-existent/";
        Resource resource = resourceFactory.create(nonExistentDir);
        assertThatThrownBy(() -> directoryService.getInfo(resource))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDirectoryContents_shouldReturnListOfResources_whenDirectoryContainsFilesAndSubdirs() {
        createTestDirectory(testDir);
        createTestDirectory(testDir + "subdir/");
        uploadTestFile(testDir + testFileName);
        uploadTestFile(testDir + "subdir/" + testFileName);

        assertThat(s3Repository.exists(userRoot + testDir + testFileName)).isTrue();
        assertThat(s3Repository.exists(userRoot + testDir + "subdir/" + testFileName)).isTrue();
        List<ResourceResponse> contents = directoryService.getDirectoryContents(testDir);
        assertThat(contents).hasSize(2);
        assertThat(contents).anyMatch(r -> r.name().equals(testFileName));
        assertThat(contents).anyMatch(r -> r.name().equals("subdir/"));
    }

    @Test
    void getDirectoryContents_shouldThrowException_whenPathIsNotDirectory() {
        String filePath = "file.txt";
        uploadTestFile(userRoot + filePath);
        assertThatThrownBy(() -> directoryService.getDirectoryContents(filePath))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getDirectoryContents_shouldThrowException_whenDirectoryDoesNotExist() {
        assertThatThrownBy(() -> directoryService.getDirectoryContents("non-existent/"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void download_shouldCreateZipOfDirectory() throws Exception {
        createTestDirectory(testDir);
        uploadTestFile(testDirPath + "file1.txt");
        uploadTestFile(testDirPath + "file2.txt");
        StreamingResponseBody zipStream = directoryService.download(testDirPath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        zipStream.writeTo(baos);
        assertThat(baos.toByteArray()).isNotEmpty();
    }

    @Test
    void delete_shouldRemoveDirectoryAndAllContents() {
        createTestDirectory(testDir);
        uploadTestFile(testDirPath + testFileName);
        createTestDirectory(testDirPath + "subdir/");
        directoryService.delete(testDirPath);
        assertThat(s3Repository.exists(testDirPath)).isFalse();
    }

    @Test
    void delete_shouldThrowException_whenDirectoryDoesNotExist() {
        assertThatThrownBy(() -> directoryService.delete("non-existent/"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void moveOrRename_shouldMoveDirectoryToNewLocation() {
        createTestDirectory(testDir);
        uploadTestFile(testDirPath + testFileName);
        String newDir = "moved-dir/";
        String fullFrom = testDirPath;
        String fullTo = userRoot + newDir;
        directoryService.moveOrRename(fullFrom, fullTo);
        assertThat(s3Repository.exists(fullFrom)).isFalse();
        assertThat(s3Repository.exists(fullTo)).isTrue();
        assertThat(s3Repository.exists(fullTo + testFileName)).isTrue();
    }

    @Test
    void ensureDirectoriesForFile_shouldCreateMissingParentDirectories() {
        String deepFilePath = userRoot + "a/b/c/" + testFileName;
        directoryService.ensureDirectoriesForFile(deepFilePath);
        assertThat(s3Repository.exists(userRoot + "a/")).isTrue();
        assertThat(s3Repository.exists(userRoot + "a/b/")).isTrue();
        assertThat(s3Repository.exists(userRoot + "a/b/c/")).isTrue();
    }
}