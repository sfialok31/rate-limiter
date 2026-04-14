package com.sfialok.ratelimiter.redis.sync;

import com.sfialok.ratelimiter.core.RateLimitDetails;
import com.sfialok.ratelimiter.core.RateLimiter;
import com.sfialok.ratelimiter.redis.NoScriptError;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.sfialok.ratelimiter.redis.RateLimitConstants.LUA_RATE_LIMITING_STRING;
import static com.sfialok.ratelimiter.redis.RateLimitResultParser.parseResult;

public class RedisTokenBucketRateLimiter implements RateLimiter {

    private final int capacity;
    private final int refillRate;
    private final int refillIntervalMs;
    private final RedisEvalExecutor redisEvalExecutor;
    private final Clock clock;
    private final AtomicReference<CompletableFuture<Boolean>> scriptLoadingFutureRef = new AtomicReference<>();
    private final String scriptSHA;

    public RedisTokenBucketRateLimiter(
        final int capacity,
        final int refillRate,
        final int refillIntervalMs,
        final Clock clock,
        final RedisEvalExecutor redisEvalExecutor) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.refillIntervalMs = refillIntervalMs;
        this.clock = clock;
        this.redisEvalExecutor =
                redisEvalExecutor instanceof SafeRedisEvalExecutor
                        ? redisEvalExecutor
                        : new SafeRedisEvalExecutor(redisEvalExecutor);
        scriptSHA = redisEvalExecutor.scriptLoad(LUA_RATE_LIMITING_STRING);
    }

    @Override
    public RateLimitDetails tryRateLimit(final String key) {
        final long now = clock.millis();

        List<Object> result;
        try {
            result = runEval(key, now);
        } catch (final NoScriptError error) {
            handleNoScriptError();
            final long newNow = clock.millis();
            result = runEval(key, newNow);
        }
        return parseResult(key, result);
    }

    @Override
    public int getLimit() {
        return capacity;
    }

    private List<Object> runEval(final String key, final long timestamp) {
        return redisEvalExecutor.evalSha(
                scriptSHA,
                List.of("rate_limit:" + key),
                List.of(
                        String.valueOf(capacity),
                        String.valueOf(refillRate),
                        String.valueOf(refillIntervalMs),
                        String.valueOf(timestamp))
        );
    }

    /**
     * Reloads lua script into redis cache with request coalescing
     */
    private void handleNoScriptError() {
        CompletableFuture<Boolean> newFuture = new CompletableFuture<>();
        final CompletableFuture<Boolean> witnessedFuture =
                scriptLoadingFutureRef.compareAndExchange(null, newFuture);
        if (witnessedFuture == null) {
            try {
                redisEvalExecutor.scriptLoad(LUA_RATE_LIMITING_STRING);
                newFuture.complete(true);
            } catch (final Exception e) {
                newFuture.completeExceptionally(e);
            } finally {
                scriptLoadingFutureRef.set(null);
            }
        } else {
            newFuture = witnessedFuture;
        }
        newFuture.join();
    }
}
