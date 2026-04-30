package com.example.cloud_storage.service.auth;

import com.example.cloud_storage.dto.user.UserDetailsImpl;
import com.example.cloud_storage.exception.user.UserNotFoundException;
import com.example.cloud_storage.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.example.cloud_storage.constant.ExceptionMessages.USER_NOT_FOUND;


@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> new UserDetailsImpl(user.getId(), user.getUsername(), user.getPassword()))
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND + " " + username));
    }
}