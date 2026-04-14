package com.sfialok.ratelimiter.redis;

public class ScriptLoadingFailedError extends RuntimeException {
    public ScriptLoadingFailedError(final String message) {
        super(message);
    }
}
