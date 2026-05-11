package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.domain.user.dto.CustomUserDetails;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws RuntimeException {

        User user = userRepository.findByEmail(email).orElseThrow( (() -> { throw new RuntimeException("USER_NOT_FOUND") ; }));
        if (user.isDeleted()) {
            throw new RuntimeException("DELETED_USER");
        }

        log.debug("Email : {}", user.getEmail());

        return new CustomUserDetails(user);
    }
}

