package com.example.cloud_storage.exception.resource;

public class MoveOperationException extends RuntimeException {
    public MoveOperationException(String message, Throwable error) {
        super(message, error);
    }
}
