package com.example.concert.domain.user.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CreateUserCommand {
    @NotBlank
    private String name;
    
    @NotBlank
    @Email
    private String email;
}
