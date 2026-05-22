package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class UserLoginHistoryService {

    private final Clock clock;
    private final UserRepository userRepository;

    public UserLoginHistoryService(Clock clock, UserRepository userRepository) {
        this.clock = clock;
        this.userRepository = userRepository;
    }

    public void recordSuccessfulLogin(User user) {
        user.recordLogin(LocalDateTime.now(clock));
        userRepository.save(user);
    }
}
