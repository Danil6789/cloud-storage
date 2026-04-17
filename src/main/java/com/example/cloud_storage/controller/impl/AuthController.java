package com.example.cloud_storage.controller.impl;

import com.example.cloud_storage.controller.api.AuthApi;
import com.example.cloud_storage.dto.auth.SignInRequest;
import com.example.cloud_storage.dto.auth.SignResponse;
import com.example.cloud_storage.dto.auth.SignUpRequest;
import com.example.cloud_storage.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
public class AuthController implements AuthApi {
    private final AuthService authService;

    @Override
    public ResponseEntity<SignResponse> signUp(@RequestBody @Valid SignUpRequest signUpRequest){
        SignResponse signResponse = authService.register(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(signResponse);
    }

    @Override
    public ResponseEntity<SignResponse> signIn(@RequestBody @Valid SignInRequest signInRequest){
        SignResponse signResponse = authService.login(signInRequest);
        return ResponseEntity.status(HttpStatus.OK).body(signResponse);
    }

    @Override
    public ResponseEntity<Void> signOut(){
        authService.logout();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}