package com.swyp.api_server.domain.user.controller;

import com.swyp.api_server.domain.user.dto.SignRequestDto;
import com.swyp.api_server.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignRequestDto signRequestDto) {
        boolean success = userService.signUp(signRequestDto);
        if(!success) {
            return ResponseEntity.badRequest().body("이미 존재하는 이메일입니다.");
        }
        return ResponseEntity.ok("회원가입 완료");
    }
}
