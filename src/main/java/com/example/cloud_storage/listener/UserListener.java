package com.example.cloud_storage.listener;

import com.example.cloud_storage.entity.User;
import com.example.cloud_storage.service.resource.DirectoryService;
import jakarta.persistence.PostPersist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserListener {
    private final DirectoryService directoryService;

    @PostPersist
    public void postPersist(User user){
        directoryService.createUserDirectory();
    }
}
