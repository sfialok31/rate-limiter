package com.sfialok.ratelimiter.redis.reactive;

import reactor.core.publisher.Mono;

import java.util.List;

public interface ReactiveRedisEvalExecutor {

    Mono<List<Object>> evalSha(
            String sha,
            List<String> keys,
            List<String> args
    );

    Mono<String> scriptLoad(String script);
}