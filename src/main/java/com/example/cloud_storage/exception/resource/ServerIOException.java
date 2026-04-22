package com.example.cloud_storage.exception.resource;

public class ServerIOException extends RuntimeException {
    public ServerIOException(String message, Throwable error) {
        super(message, error);
    }
}
