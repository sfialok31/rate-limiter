package com.sfialok.ratelimiter.redis.reactive;

import com.sfialok.ratelimiter.core.RateLimitDetails;
import com.sfialok.ratelimiter.redis.NoScriptError;
import com.sfialok.ratelimiter.redis.ScriptLoadingFailedError;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.sfialok.ratelimiter.redis.RateLimitConstants.LUA_RATE_LIMITING_STRING;
import static com.sfialok.ratelimiter.redis.RateLimitResultParser.parseResult;

public class ReactiveRedisTokenBucketRateLimiter implements ReactiveRateLimiter {

    private final int capacity;
    private final int refillRate;
    private final int refillIntervalMs;
    private final ReactiveRedisEvalExecutor reactiveRedisEvalExecutor;
    private final Clock clock;
    private final AtomicReference<Mono<String>> scriptSHAMonoRef;


    public ReactiveRedisTokenBucketRateLimiter(
            final int capacity,
            final int refillRate,
            final int refillIntervalMs,
            final Clock clock,
            final ReactiveRedisEvalExecutor reactiveRedisEvalExecutor
    ) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.refillIntervalMs = refillIntervalMs;
        this.clock = clock;
        this.reactiveRedisEvalExecutor =
                reactiveRedisEvalExecutor instanceof ReactiveSafeRedisEvalExecutor ? reactiveRedisEvalExecutor :
                        new ReactiveSafeRedisEvalExecutor(reactiveRedisEvalExecutor);
        scriptSHAMonoRef = new AtomicReference<>(
                reactiveRedisEvalExecutor
                        .scriptLoad(LUA_RATE_LIMITING_STRING)
                        .cache()
        );
    }

    @Override
    public Mono<RateLimitDetails> tryRateLimit(final String key) {
        final Mono<String> scriptSHAMono = scriptSHAMonoRef.get();
        return scriptSHAMono
                .onErrorResume(ex -> Mono.error(
                        new ScriptLoadingFailedError("Script Loading Failed: " + ex.getMessage())))
                .flatMap(scriptSHA -> evalScriptSHA(key, clock.millis(), scriptSHA))
                .onErrorResume(ex -> {
                    if (ex instanceof NoScriptError || ex instanceof ScriptLoadingFailedError) {
                        // reload comparing with initial version to prevent thundering herd/cache stampede
                        scriptSHAMonoRef.compareAndSet(
                                scriptSHAMono,
                                Mono.defer(() -> reactiveRedisEvalExecutor.scriptLoad(LUA_RATE_LIMITING_STRING)).cache()
                        );
                        return scriptSHAMonoRef.get()
                                .flatMap(newSHA -> evalScriptSHA(key, clock.millis(), newSHA));
                    }
                    return Mono.error(ex);
                })
                .map(result -> parseResult(key, result));
    }

    @Override
    public int getLimit() {
        return this.capacity;
    }

    private Mono<List<Object>> evalScriptSHA(final String key, final long now, final String scriptSHA) {
        return reactiveRedisEvalExecutor.evalSha(
                scriptSHA,
                List.of("rate_limit:" + key),
                List.of(
                        String.valueOf(capacity),
                        String.valueOf(refillRate),
                        String.valueOf(refillIntervalMs),
                        String.valueOf(now))
        );
    }
}
