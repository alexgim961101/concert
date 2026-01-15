package com.example.concert.domain.queue.usecase;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String token) {
        super("Token has expired: " + token);
    }
}
