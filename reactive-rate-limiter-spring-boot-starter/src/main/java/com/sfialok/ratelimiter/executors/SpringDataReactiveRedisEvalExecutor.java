package com.sfialok.ratelimiter.executors;

import com.sfialok.ratelimiter.redis.reactive.ReactiveRedisEvalExecutor;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SpringDataReactiveRedisEvalExecutor implements ReactiveRedisEvalExecutor {
    final ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    public SpringDataReactiveRedisEvalExecutor(final ReactiveRedisConnectionFactory reactiveRedisConnectionFactory) {
        this.reactiveRedisConnectionFactory = reactiveRedisConnectionFactory;
    }

    @Override
    public Mono<List<Object>> evalSha(final String sha, final List<String> keys, final List<String> args) {
        final List<String> keysAndArgs = new ArrayList<>();
        keysAndArgs.addAll(keys);
        keysAndArgs.addAll(args);
        final ByteBuffer[] keysAndArgsArray = new ByteBuffer[keysAndArgs.size()];
        for (int i = 0; i < keysAndArgs.size(); i++) {
            keysAndArgsArray[i] = ByteBuffer.wrap(keysAndArgs.get(i).getBytes(StandardCharsets.UTF_8));
        }
        return reactiveRedisConnectionFactory.getReactiveConnection()
                .scriptingCommands()
                .evalSha(sha, ReturnType.MULTI, keys.size(), keysAndArgsArray)
                .next()
                .map(result -> (List<Object>) result);
    }

    @Override
    public Mono<String> scriptLoad(final String script) {
        return reactiveRedisConnectionFactory.getReactiveConnection()
                .scriptingCommands()
                .scriptLoad(ByteBuffer.wrap(script.getBytes(StandardCharsets.UTF_8)));
    }
}
