package com.example.concert.domain.user.application.port.out;

import com.example.concert.domain.user.domain.entity.User;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
}
