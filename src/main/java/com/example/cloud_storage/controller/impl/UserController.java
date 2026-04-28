package com.example.cloud_storage.controller.impl;

import com.example.cloud_storage.controller.UserApi;
import com.example.cloud_storage.dto.UserDetailsImpl;
import com.example.cloud_storage.dto.user.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApi {

    @Override
    public ResponseEntity<UserResponse> getUser(@AuthenticationPrincipal UserDetailsImpl userDetails){
        String username = userDetails.getUsername();
        UserResponse response = new UserResponse(username);

        return ResponseEntity.ok(response);
    }
}