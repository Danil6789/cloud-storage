package com.example.cloud_storage.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PathValidator.class)
public @interface ValidPath {
    String message() default "Invalid path format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}