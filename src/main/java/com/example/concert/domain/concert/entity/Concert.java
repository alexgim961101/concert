package com.example.concert.domain.concert.entity;

import java.time.LocalDateTime;

/**
 * 콘서트 도메인 객체 (순수 Java, No JPA)
 */
public class Concert {
    private final Long id;
    private final String title;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Concert(String title, String description) {
        this(null, title, description, null, null);
    }

    public Concert(Long id, String title, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
