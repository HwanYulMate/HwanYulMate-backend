package com.swyp.api_server.domain.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignRequestDto {
    private String email;
    private String password;
    private String userName;
}
