package com.sfialok.ratelimiter.redis;

public class NoScriptError extends RuntimeException {
    public NoScriptError(final String message) {
        super(message);
    }
}
