package com.sfialok.ratelimiter.redis.sync;

import java.util.List;

public interface RedisEvalExecutor {
    List<Object> evalSha(
            String sha,
            List<String> keys,
            List<String> args
    );

    String scriptLoad(String script);
}
