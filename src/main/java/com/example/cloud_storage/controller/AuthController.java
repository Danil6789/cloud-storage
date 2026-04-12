package com.example.cloud_storage.controller;

import com.example.cloud_storage.dto.auth.SignResponse;
import com.example.cloud_storage.dto.auth.SignUpRequest;
import com.example.cloud_storage.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<SignResponse> signUp(@RequestBody @Valid SignUpRequest signUpRequest){
        SignResponse signResponse = authService.register(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(signResponse);
    }
}