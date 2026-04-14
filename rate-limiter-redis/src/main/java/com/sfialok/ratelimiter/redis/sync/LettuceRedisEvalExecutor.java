package com.sfialok.ratelimiter.redis.sync;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.List;

public class LettuceRedisEvalExecutor implements RedisEvalExecutor {
    private final RedisCommands<String, String> commands;

    public LettuceRedisEvalExecutor(final RedisCommands<String, String> commands) {
        this.commands = commands;
    }

    @Override
    public List<Object> evalSha(final String scriptSHA, final List<String> keys, final List<String> args) {
        return commands.evalsha(
                scriptSHA,
                ScriptOutputType.MULTI,
                keys.toArray(new String[]{}),
                args.toArray(new String[]{})
        );
    }

    @Override
    public String scriptLoad(final String script) {
        return commands.scriptLoad(script);
    }
}
