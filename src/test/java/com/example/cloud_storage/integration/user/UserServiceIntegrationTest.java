package com.example.cloud_storage.integration.user;

import com.example.cloud_storage.entity.User;
import com.example.cloud_storage.exception.user.UserAlreadyExistsException;
import com.example.cloud_storage.exception.user.UserNotFoundException;
import com.example.cloud_storage.repository.resource.S3Repository;
import com.example.cloud_storage.repository.user.UserRepository;
import com.example.cloud_storage.service.auth.UserService;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.example.cloud_storage.constant.ExceptionMessages.USER_ALREADY_EXISTS;
import static com.example.cloud_storage.constant.ExceptionMessages.USER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15");

    @MockitoBean
    private S3Repository s3Repository;

    @MockitoBean
    private MinioClient minioClient;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUser_shouldSaveNewUserToDatabase(){
        User user = new User();
        user.setUsername("bruce");
        user.setPassword("batman");

        User createdUser = userService.createUser(user);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();

        User foundUser = userRepository.findByUsername("bruce").orElse(null);
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo("bruce");
    }

    @Test
    void createUser_shouldThrowUserAlreadyExistsException(){
        User user1 = new User();
        user1.setUsername("bruce");
        user1.setPassword("batman");

        User user2 = new User();
        user2.setUsername("bruce");
        user2.setPassword("rich");

        userService.createUser(user1);

        assertThatThrownBy(() -> userService.createUser(user2))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage(USER_ALREADY_EXISTS);
    }

    @Test
    void getUserByUsername_shouldReturnSavedUser(){
        User user = new User();
        user.setUsername("bruce");
        user.setPassword("batman");
        User createdUser = userService.createUser(user);

        User foundUser = userService.getUserByUsername("bruce");

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo(createdUser.getUsername());
    }

    @Test
    void getUserByUsername_shouldThrowUserNotFoundException(){
        assertThatThrownBy(() -> userService.getUserByUsername("bruce"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage(USER_NOT_FOUND);
    }
}