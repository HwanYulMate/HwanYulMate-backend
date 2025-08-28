package com.swyp.api_server.domain.user.service;

import com.swyp.api_server.domain.user.dto.SignRequestDto;

public interface UserService {
    boolean signUp(SignRequestDto signRequestDto);
}
