package com.sfialok.ratelimiter.redis.reactive;

import com.sfialok.ratelimiter.redis.NoScriptError;
import org.apache.commons.lang3.exception.ExceptionUtils;
import reactor.core.publisher.Mono;

import java.util.List;

public class ReactiveSafeRedisEvalExecutor implements ReactiveRedisEvalExecutor {
    private final ReactiveRedisEvalExecutor delegate;

    public ReactiveSafeRedisEvalExecutor(final ReactiveRedisEvalExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<List<Object>> evalSha(final String sha, final List<String> keys, final List<String> args) {
        return delegate.evalSha(sha, keys, args)
                .onErrorMap(
                        ex -> {
                            for (final Throwable t : ExceptionUtils.getThrowables(ex)) {
                                if (t.getMessage() != null && t.getMessage().contains("NOSCRIPT")) {
                                    return new NoScriptError("Redis Script missing on server");
                                }
                            }
                            return ex;
                        }
                );
    }

    @Override
    public Mono<String> scriptLoad(final String script) {
        return delegate.scriptLoad(script);
    }
}
