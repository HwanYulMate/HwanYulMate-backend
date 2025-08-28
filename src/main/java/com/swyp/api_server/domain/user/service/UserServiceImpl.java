package com.swyp.api_server.domain.user.service;


import com.swyp.api_server.domain.user.dto.SignRequestDto;
import com.swyp.api_server.domain.user.repository.UserRepository;
import com.swyp.api_server.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean signUp(SignRequestDto signRequestDto) {
        if(userRepository.existsByEmail(signRequestDto.getEmail())) {
            return false;
        }

        User user = User.builder()
                .email(signRequestDto.getEmail())
                .password(passwordEncoder.encode(signRequestDto.getPassword()))
                .provider("local")
                .role("ROLE_USER")
                .userName(signRequestDto.getUserName())
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        return true;

    }
}
