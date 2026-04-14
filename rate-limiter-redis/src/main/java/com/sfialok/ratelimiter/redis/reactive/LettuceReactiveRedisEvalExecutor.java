package com.sfialok.ratelimiter.redis.reactive;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Mono;

import java.util.List;

public class LettuceReactiveRedisEvalExecutor implements ReactiveRedisEvalExecutor {

    private final RedisReactiveCommands<String, String> commands;

    public LettuceReactiveRedisEvalExecutor(
            final RedisReactiveCommands<String, String> commands) {
        this.commands = commands;
    }


    @Override
    public Mono<List<Object>> evalSha(final String sha, final List<String> keys, final List<String> args) {
        return commands.evalsha(
                        sha,
                        ScriptOutputType.MULTI,
                        keys.toArray(new String[0]),
                        args.toArray(new String[0])
                )
                .next()
                .map(result -> (List<Object>) result);
    }

    @Override
    public Mono<String> scriptLoad(final String script) {
        return commands.scriptLoad(script);
    }
}
