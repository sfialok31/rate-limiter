package com.sfialok.ratelimiter.redis.reactive;

import com.sfialok.ratelimiter.core.RateLimitDetails;
import reactor.core.publisher.Mono;

public interface ReactiveRateLimiter {
    Mono<RateLimitDetails> tryRateLimit(final String key);
    int getLimit();
}
