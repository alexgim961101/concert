package com.example.concert.domain.concert.usecase;

public class ConcertNotFoundException extends RuntimeException {
    public ConcertNotFoundException(Long concertId) {
        super("Concert not found: " + concertId);
    }
}
