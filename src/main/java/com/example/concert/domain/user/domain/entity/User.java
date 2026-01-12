package com.example.concert.domain.user.domain.entity;

import com.example.concert.common.domain.BaseEntity;
import com.example.concert.domain.user.domain.vo.Email;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @Embedded
    private Email email;
    
    // 생성자
    protected User() {} // JPA용
    
    public User(String name, Email email) {
        this.name = name;
        this.email = email;
    }
    
    // 비즈니스 로직 메서드
    public void changeName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        this.name = newName;
    }
    
    public void changeEmail(Email newEmail) {
        if (newEmail == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        this.email = newEmail;
    }
    
    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public Email getEmail() { return email; }
}
