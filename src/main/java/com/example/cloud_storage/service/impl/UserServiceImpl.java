package com.example.cloud_storage.service.impl;

import com.example.cloud_storage.entity.User;
import com.example.cloud_storage.exception.user.UserAlreadyExistsException;
import com.example.cloud_storage.exception.user.UserNotFoundException;
import com.example.cloud_storage.repository.UserRepository;
import com.example.cloud_storage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//TODO: реализовать через контстанту сообщение об ошибке - import static ru.masnaviev.cloudstorage.constants.ErrorMessages.USER_ALREADY_EXISTS;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User createUser(User user){
        try{
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);

        }catch(DataIntegrityViolationException e){
            throw new UserAlreadyExistsException("Пользователь с таким username уже существует"); //TODO: Вынести сообщение в константу
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUsername(String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Нет такого username")); //TODO: Вынести сообщение в константу
    }
}