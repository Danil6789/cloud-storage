package com.example.cloud_storage.service;

import com.example.cloud_storage.entity.User;

public interface UserService {
    User createUser(User user);
    User getUserByUsername(String username);
}