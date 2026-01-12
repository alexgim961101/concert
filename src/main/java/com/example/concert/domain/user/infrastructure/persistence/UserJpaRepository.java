package com.example.concert.domain.user.infrastructure.persistence;

import com.example.concert.domain.user.application.port.out.UserRepositoryPort;
import com.example.concert.domain.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserJpaRepository implements UserRepositoryPort {
    private final UserJpaRepositoryAdapter userJpaRepositoryAdapter;
    
    @Override
    public User save(User user) {
        return userJpaRepositoryAdapter.save(user);
    }
    
    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepositoryAdapter.findById(id);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepositoryAdapter.findByEmail(email);
    }
}
