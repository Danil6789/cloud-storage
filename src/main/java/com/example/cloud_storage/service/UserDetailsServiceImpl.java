package com.example.cloud_storage.service;

import com.example.cloud_storage.dto.UserDetailsImpl;
import com.example.cloud_storage.entity.User;
import com.example.cloud_storage.exception.UserNotFoundException;
import com.example.cloud_storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователянет" + username));

        return new UserDetailsImpl(user.getUsername(), user.getPassword());
    }
}