package com.hackernews.model;

public class RateLimitException extends RuntimeException {
    private final int retryAfter;

    public RateLimitException(String message, int retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public int getRetryAfter() {
        return retryAfter;
    }
}