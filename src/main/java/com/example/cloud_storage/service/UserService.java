package com.example.cloud_storage.service;

import com.example.cloud_storage.entity.User;
import com.example.cloud_storage.exception.UserAlreadyExistsException;
import com.example.cloud_storage.exception.UserNotFoundException;
import com.example.cloud_storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//TODO: реализовать через контстанту сообщение об ошибке - import static ru.masnaviev.cloudstorage.constants.ErrorMessages.USER_ALREADY_EXISTS;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(User user){
        try{
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);

        }catch(DataIntegrityViolationException e){
            throw new UserAlreadyExistsException("Пользователь с таким username уже существует");
        }
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String login){
        return userRepository.findByUsername(login)
                .orElseThrow(() -> new UserNotFoundException("Нет такого username"));
    }


}