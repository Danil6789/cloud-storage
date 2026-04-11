package com.example.cloud_storage.service;

import com.example.cloud_storage.dto.UserDetailsImpl;
import com.example.cloud_storage.entity.User;
import com.example.cloud_storage.exception.UserNotFoundException;
import com.example.cloud_storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователянет" + username));
        UserDetailsImpl userDetails = new UserDetailsImpl(user.getUsername(), user.getPassword());

        return userDetails;
    }
}