package com.example.cloud_storage.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PathValidator implements ConstraintValidator<ValidPath, String> {

    private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile("[<>:\"|?*\\\\]");

    @Override
    public boolean isValid(String path, ConstraintValidatorContext context) {
        if (path == null
                || path.isBlank()
                || path.contains("..")
                || INVALID_CHARS_PATTERN.matcher(path).find()
                || path.length() > 1024
        ) {
            return false;
        }

        return true;
    }
}