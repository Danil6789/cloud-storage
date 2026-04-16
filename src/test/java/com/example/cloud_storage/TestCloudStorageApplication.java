package com.example.cloud_storage;

import org.springframework.boot.SpringApplication;
import org.testcontainers.utility.TestcontainersConfiguration;

public class TestCloudStorageApplication {

	public static void main(String[] args) {
		SpringApplication.from(CloudStorageApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
