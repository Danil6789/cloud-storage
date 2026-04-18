package com.example.cloud_storage.service;

import com.example.cloud_storage.dto.auth.SignInRequest;
import com.example.cloud_storage.dto.auth.SignResponse;
import com.example.cloud_storage.dto.auth.SignUpRequest;

public interface AuthService {
    SignResponse register(SignUpRequest signUpRequest);

    SignResponse login(SignInRequest signInRequest);

    void logout();
}
