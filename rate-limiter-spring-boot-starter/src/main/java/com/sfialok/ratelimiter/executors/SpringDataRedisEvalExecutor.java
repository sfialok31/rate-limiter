package com.sfialok.ratelimiter.executors;

import com.sfialok.ratelimiter.redis.sync.RedisEvalExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SpringDataRedisEvalExecutor implements RedisEvalExecutor {
    final RedisConnectionFactory redisConnectionFactory;

    public SpringDataRedisEvalExecutor(final RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public List<Object> evalSha(final String sha, final List<String> keys, final List<String> args) {
        final List<String> keysAndArgs = new ArrayList<>();
        keysAndArgs.addAll(keys);
        keysAndArgs.addAll(args);
        final byte[][] keysAndArgsArray = new byte[keysAndArgs.size()][];
        for (int i = 0; i < keysAndArgs.size(); i++) {
            keysAndArgsArray[i] = keysAndArgs.get(i).getBytes(StandardCharsets.UTF_8);
        }
        return redisConnectionFactory.getConnection()
                .scriptingCommands()
                .evalSha(sha, ReturnType.MULTI, keys.size(), keysAndArgsArray);
    }

    @Override
    public String scriptLoad(final String script) {
        return redisConnectionFactory.getConnection()
                .scriptingCommands()
                .scriptLoad(script.getBytes(StandardCharsets.UTF_8));
    }
}
