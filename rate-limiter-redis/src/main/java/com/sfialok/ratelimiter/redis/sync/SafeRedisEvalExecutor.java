package com.sfialok.ratelimiter.redis.sync;

import com.sfialok.ratelimiter.redis.NoScriptError;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;

public class SafeRedisEvalExecutor implements RedisEvalExecutor {
    private final RedisEvalExecutor delegate;
    public SafeRedisEvalExecutor(final RedisEvalExecutor redisEvalExecutor) {
        this.delegate = redisEvalExecutor;
    }

    @Override
    public List<Object> evalSha(final String sha, final List<String> keys, final List<String> args) {
        try {
            return delegate.evalSha(sha, keys, args);
        } catch (final Exception ex) {
            for (final Throwable t : ExceptionUtils.getThrowables(ex)) {
                if (t.getMessage() != null && t.getMessage().contains("NOSCRIPT")) {
                    throw new NoScriptError("Redis Script missing on server");
                }
            }
            throw ex;
        }
    }

    @Override
    public String scriptLoad(final String script) {
        return delegate.scriptLoad(script);
    }
}
