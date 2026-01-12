package com.example.concert.domain.user.application.service;

import com.example.concert.common.exception.ResourceNotFoundException;
import com.example.concert.domain.user.application.port.in.UserUseCase;
import com.example.concert.domain.user.application.port.out.UserRepositoryPort;
import com.example.concert.domain.user.domain.entity.User;
import com.example.concert.domain.user.domain.vo.Email;
import com.example.concert.domain.user.presentation.dto.request.CreateUserCommand;
import com.example.concert.domain.user.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements UserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    
    @Override
    public UserResponse createUser(CreateUserCommand command) {
        // 비즈니스 로직
        Email email = new Email(command.getEmail());
        User user = new User(command.getName(), email);
        
        User savedUser = userRepositoryPort.save(user);
        return UserResponse.from(savedUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepositoryPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return UserResponse.from(user);
    }
}
