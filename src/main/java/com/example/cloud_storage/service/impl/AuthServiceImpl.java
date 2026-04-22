package com.example.cloud_storage.service.impl;

import com.example.cloud_storage.dto.auth.SignInRequest;
import com.example.cloud_storage.dto.auth.SignResponse;
import com.example.cloud_storage.dto.auth.SignUpRequest;
import com.example.cloud_storage.entity.User;
import com.example.cloud_storage.mapper.UserMapper;
import com.example.cloud_storage.service.AuthService;
import com.example.cloud_storage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    @Override
    public SignResponse register(SignUpRequest signUpRequest){
        User user = userService.createUser(userMapper.toEntity(signUpRequest));
        authenticate(signUpRequest.getUsername(), signUpRequest.getPassword());

        return userMapper.toResponseDto(user);
    }

    @Override
    public SignResponse login(SignInRequest signInRequest){
        authenticate(signInRequest.getUsername(), signInRequest.getPassword());

        return userMapper.toResponseDto(signInRequest.getUsername());
    }

    private void authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Override
    public void logout(){//TODO: Мертвый код. До него запрос не дойдёт
        SecurityContextHolder.clearContext();
    }
}