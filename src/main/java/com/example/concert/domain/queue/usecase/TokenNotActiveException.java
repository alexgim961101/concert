package com.example.concert.domain.queue.usecase;

public class TokenNotActiveException extends RuntimeException {
    public TokenNotActiveException(String token) {
        super("Token is not active: " + token);
    }
}
