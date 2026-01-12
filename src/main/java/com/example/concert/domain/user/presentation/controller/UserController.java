package com.example.concert.domain.user.presentation.controller;

import com.example.concert.common.dto.ApiResponse;
import com.example.concert.domain.user.application.port.in.UserUseCase;
import com.example.concert.domain.user.presentation.dto.request.CreateUserCommand;
import com.example.concert.domain.user.presentation.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase userUseCase;
    
    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserCommand command) {
        return ApiResponse.success(userUseCase.createUser(command));
    }
    
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success(userUseCase.getUser(id));
    }
}
