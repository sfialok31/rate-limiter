package com.sfialok.ratelimiter.core;

public interface RateLimiter {
    RateLimitDetails tryRateLimit(final String key);
}
