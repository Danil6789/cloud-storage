package com.example.cloud_storage.exception.resource;

public class S3OperationException extends RuntimeException {
    public S3OperationException(String message, Throwable error) {
        super(message, error);
    }
}
