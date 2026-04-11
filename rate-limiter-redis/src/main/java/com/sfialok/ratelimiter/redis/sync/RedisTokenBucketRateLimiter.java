package com.sfialok.ratelimiter.redis.sync;

import com.sfialok.ratelimiter.core.RateLimitDetails;
import com.sfialok.ratelimiter.core.RateLimiter;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Clock;
import java.util.List;

import static com.sfialok.ratelimiter.redis.RateLimitResultParser.parseResult;

public class RedisTokenBucketRateLimiter implements RateLimiter {

    private final int capacity;
    private final int refillRate;
    private final int refillIntervalMs;
    private final RedisCommands<String, String> syncCommands;
    private final Clock clock;
    public final String LUA_RATE_LIMITING_STRING = """
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
    private final String scriptSHA;

    public RedisTokenBucketRateLimiter(
        final int capacity,
        final int refillRate,
        final int refillIntervalMs,
        final Clock clock,
        final RedisCommands<String, String> syncCommands) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.refillIntervalMs = refillIntervalMs;
        this.clock = clock;
        this.syncCommands = syncCommands;
        scriptSHA = this.syncCommands.scriptLoad(LUA_RATE_LIMITING_STRING);
    }

    @Override
    public RateLimitDetails tryRateLimit(final String key) {
        final long now = clock.millis();
        final List<Object> result = syncCommands.evalsha(
                scriptSHA,
                ScriptOutputType.MULTI,
                new String[]{"rate_limit:" + key},
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(refillIntervalMs),
                String.valueOf(now)
        );
        return parseResult(key, result);
    }
}
