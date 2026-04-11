package com.sfialok.ratelimiter.redis.reactive;

import com.sfialok.ratelimiter.core.RateLimitDetails;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.List;

import static com.sfialok.ratelimiter.redis.RateLimitResultParser.parseResult;

public class ReactiveRedisTokenBucketRateLimiter {

    private final int capacity;
    private final int refillRate;
    private final int refillInterval;
    private final RedisReactiveCommands<String, String> reactiveCommands;
    private final Clock clock;
    private final String scriptSHA;

    public ReactiveRedisTokenBucketRateLimiter(
            final int capacity,
            final int refillRate,
            final int refillInterval,
            final Clock clock,
            final RedisReactiveCommands<String, String> reactiveCommands,
            final String luaScript
            ) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.refillInterval = refillInterval;
        this.clock = clock;
        this.reactiveCommands = reactiveCommands;

        // scriptLoad also returns a Mono in reactive API
        this.scriptSHA = this.reactiveCommands.scriptLoad(luaScript).block();
    }

    public Mono<RateLimitDetails> tryRateLimit(final String key) {
        final long now = clock.millis();

        return reactiveCommands.evalsha(
                        scriptSHA,
                        ScriptOutputType.MULTI,
                        new String[]{"rate_limit:" + key},
                        String.valueOf(capacity),
                        String.valueOf(refillRate),
                        String.valueOf(refillInterval),
                        String.valueOf(now)
                )
                .next()
                .map(result -> (List<Object>) result)
                .map(result -> parseResult(key, result));
    }
}
