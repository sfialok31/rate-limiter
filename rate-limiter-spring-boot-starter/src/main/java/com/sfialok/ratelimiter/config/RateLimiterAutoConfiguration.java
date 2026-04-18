package com.sfialok.ratelimiter.config;

import com.sfialok.ratelimiter.core.RateLimiter;
import com.sfialok.ratelimiter.executors.SpringDataRedisEvalExecutor;
import com.sfialok.ratelimiter.filter.RateLimiterFilter;
import com.sfialok.ratelimiter.properties.RateLimiterProperties;
import com.sfialok.ratelimiter.redis.sync.LettuceRedisEvalExecutor;
import com.sfialok.ratelimiter.redis.sync.RedisEvalExecutor;
import com.sfialok.ratelimiter.redis.sync.RedisTokenBucketRateLimiter;
import com.sfialok.ratelimiter.resolver.IPKeyResolver;
import com.sfialok.ratelimiter.resolver.KeyResolver;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Clock;

@AutoConfiguration
@AutoConfigureAfter(DataRedisAutoConfiguration.class)
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public KeyResolver keyResolver() {
        return new IPKeyResolver();
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisEvalExecutor springExecutor(final RedisConnectionFactory factory) {
        return new SpringDataRedisEvalExecutor(factory);
    }

    @Bean
    @ConditionalOnBean(RedisCommands.class)
    @ConditionalOnMissingBean
    public RedisEvalExecutor lettuceExecutor(final RedisCommands<String, String> commands) {
        return new LettuceRedisEvalExecutor(commands);
    }

    @Bean
    @ConditionalOnBean(RedisEvalExecutor.class)
    public RateLimiter rateLimiter(
            final RedisEvalExecutor redisEvalExecutor,
            final RateLimiterProperties rateLimiterProperties) {
        return new RedisTokenBucketRateLimiter(
                rateLimiterProperties.getCapacity(),
                rateLimiterProperties.getRefillRate(),
                rateLimiterProperties.getRefillIntervalMs(),
                Clock.systemDefaultZone(),
                redisEvalExecutor
        );
    }

    @Bean
    @ConditionalOnBean(RateLimiterFilter.class)
    @ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Filter rateLimitingFilter(final RateLimiter rateLimiter,
                                     final KeyResolver keyResolver) {
        return new RateLimiterFilter(rateLimiter, keyResolver);
    }
}
