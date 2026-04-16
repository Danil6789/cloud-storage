package com.example.cloud_storage.service.auth;

import com.example.cloud_storage.dto.auth.SignInRequest;
import com.example.cloud_storage.dto.auth.SignResponse;
import com.example.cloud_storage.dto.auth.SignUpRequest;
import com.example.cloud_storage.entity.User;
import com.example.cloud_storage.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    public SignResponse register(SignUpRequest signUpRequest){
        User user = userService.createUser(userMapper.toEntity(signUpRequest));
        authenticate(signUpRequest.getUsername(), signUpRequest.getPassword());

        return userMapper.toDtoResponse(user);
    }

    public SignResponse login(SignInRequest signInRequest){
        authenticate(signInRequest.getUsername(), signInRequest.getPassword());

        return userMapper.toDtoResponse(signInRequest.getUsername());
    }

    private void authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public void logout(){
        SecurityContextHolder.clearContext();
    }
}
