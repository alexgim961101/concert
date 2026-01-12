package com.example.concert.domain.user.application.port.in;

import com.example.concert.domain.user.presentation.dto.request.CreateUserCommand;
import com.example.concert.domain.user.presentation.dto.response.UserResponse;

public interface UserUseCase {
    UserResponse createUser(CreateUserCommand command);
    UserResponse getUser(Long id);
}
