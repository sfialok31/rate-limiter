package com.sfialok.ratelimiter.redis;

import com.sfialok.ratelimiter.core.RateLimitDetails;

import java.util.List;

public class RateLimitResultParser {

    public static RateLimitDetails parseResult(final String key, final List<Object> result) {
        final boolean allowed = ((Number) result.getFirst()).intValue() == 1;
        final int tokensRemaining = ((Number) result.get(1)).intValue();
        final long retryAfter = ((Number) result.get(2)).longValue();
        final long retryAt = ((Number) result.get(2)).longValue();

        return new RateLimitDetails(key, tokensRemaining, allowed, retryAfter, retryAt);
    }
}
