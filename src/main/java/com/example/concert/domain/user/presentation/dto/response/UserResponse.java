package com.example.concert.domain.user.presentation.dto.response;

import com.example.concert.domain.user.domain.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    
    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.name = user.getName();
        response.email = user.getEmail().getValue();
        return response;
    }
}
