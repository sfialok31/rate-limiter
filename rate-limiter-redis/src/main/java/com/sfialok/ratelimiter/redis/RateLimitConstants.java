package com.sfialok.ratelimiter.redis;

public class RateLimitConstants {
    public static final String LUA_RATE_LIMITING_STRING = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local refill_interval_ms = tonumber(ARGV[3])
            local now = tonumber(ARGV[4])

            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1])
            local last_refill = tonumber(bucket[2])

            if tokens == nil then
                tokens = capacity
                last_refill = now
            end

            local time_passed = now - last_refill
            local refills = math.floor(time_passed / refill_interval_ms)
            if refills > 0 then
                tokens = math.min(capacity, tokens + refills * refill_rate)
                last_refill = last_refill + refills * refill_interval_ms
            end

            local allowed = 0
            local retryAfter = -1
            local retryAt = -1
            if tokens >= 1 then
                tokens = tokens - 1
                allowed = 1
            else
                retryAfter = refill_interval_ms - time_passed
                retryAt = now + retryAfter
            end

            redis.call('HSET', key, 'tokens', tokens, 'last_refill', last_refill)

            local ttl = math.ceil((capacity / refill_rate) * refill_interval_ms)
            redis.call('PEXPIRE', key, ttl)

            return {allowed, tokens, retryAfter, retryAt}
    """;
}
