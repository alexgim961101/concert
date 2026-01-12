package com.example.concert.domain.user.domain.repository;

import com.example.concert.domain.user.domain.entity.User;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    void delete(User user);
}
