package com.sfialok.ratelimiter.core;

public record RateLimitDetails(String key, int requestsRemaining, boolean allowed,
                               long retryAfterMs, long resetAtEpochMs) {
}