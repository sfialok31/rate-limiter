package com.sfialok.ratelimiter.reactive.config;

import com.sfialok.ratelimiter.reactive.executors.SpringDataReactiveRedisEvalExecutor;
import com.sfialok.ratelimiter.reactive.filter.RateLimiterWebFilter;
import com.sfialok.ratelimiter.reactive.properties.RateLimiterProperties;
import com.sfialok.ratelimiter.redis.reactive.LettuceReactiveRedisEvalExecutor;
import com.sfialok.ratelimiter.redis.reactive.ReactiveRateLimiter;
import com.sfialok.ratelimiter.redis.reactive.ReactiveRedisEvalExecutor;
import com.sfialok.ratelimiter.redis.reactive.ReactiveRedisTokenBucketRateLimiter;
import com.sfialok.ratelimiter.reactive.resolver.IPKeyResolver;
import com.sfialok.ratelimiter.reactive.resolver.KeyResolver;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.web.server.WebFilter;

import java.time.Clock;

@AutoConfiguration
@ConditionalOnClass({WebFilter.class, ReactiveRedisConnectionFactory.class})
@AutoConfigureAfter(DataRedisReactiveAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableConfigurationProperties(RateLimiterProperties.class)
public class ReactiveRateLimiterAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public KeyResolver keyResolver() {
        return new IPKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveRedisEvalExecutor springExecutor(final ReactiveRedisConnectionFactory factory) {
        return new SpringDataReactiveRedisEvalExecutor(factory);
    }

    @Bean
    @ConditionalOnBean(RedisReactiveCommands.class)
    @ConditionalOnMissingBean
    public ReactiveRedisEvalExecutor lettuceExecutor(final RedisReactiveCommands<String, String> commands) {
        return new LettuceReactiveRedisEvalExecutor(commands);
    }

    @Bean
    public ReactiveRateLimiter rateLimiter(
            final ReactiveRedisEvalExecutor reactiveRedisEvalExecutor,
            final RateLimiterProperties rateLimiterProperties) {
        return new ReactiveRedisTokenBucketRateLimiter(
                rateLimiterProperties.getCapacity(),
                rateLimiterProperties.getRefillRate(),
                rateLimiterProperties.getRefillIntervalMs(),
                Clock.systemDefaultZone(),
                reactiveRedisEvalExecutor
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limiter-reactive", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebFilter rateLimitingWebFilter(final ReactiveRateLimiter reactiveRateLimiter,
                                           final KeyResolver keyResolver) {
        return new RateLimiterWebFilter(reactiveRateLimiter, keyResolver);
    }
}
