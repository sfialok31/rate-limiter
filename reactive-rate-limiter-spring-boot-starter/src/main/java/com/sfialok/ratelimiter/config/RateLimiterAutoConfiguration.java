package com.sfialok.ratelimiter.config;

import com.sfialok.ratelimiter.executors.SpringDataReactiveRedisEvalExecutor;
import com.sfialok.ratelimiter.filter.RateLimiterWebFilter;
import com.sfialok.ratelimiter.properties.RateLimiterProperties;
import com.sfialok.ratelimiter.redis.reactive.LettuceReactiveRedisEvalExecutor;
import com.sfialok.ratelimiter.redis.reactive.ReactiveRateLimiter;
import com.sfialok.ratelimiter.redis.reactive.ReactiveRedisEvalExecutor;
import com.sfialok.ratelimiter.redis.reactive.ReactiveRedisTokenBucketRateLimiter;
import com.sfialok.ratelimiter.resolver.IPKeyResolver;
import com.sfialok.ratelimiter.resolver.KeyResolver;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.web.server.WebFilter;

import java.time.Clock;

@Configuration
@AutoConfiguration
@ConditionalOnClass(WebFilter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public KeyResolver keyResolver() {
        return new IPKeyResolver();
    }

    @Bean
    @ConditionalOnBean(ReactiveRedisConnectionFactory.class)
    @ConditionalOnMissingBean(ReactiveRedisEvalExecutor.class)
    public ReactiveRedisEvalExecutor springExecutor(final ReactiveRedisConnectionFactory factory) {
        return new SpringDataReactiveRedisEvalExecutor(factory);
    }

    @Bean
    @ConditionalOnBean(RedisReactiveCommands.class)
    @ConditionalOnMissingBean(ReactiveRedisEvalExecutor.class)
    public ReactiveRedisEvalExecutor lettuceExecutor(final RedisReactiveCommands<String, String> commands) {
        return new LettuceReactiveRedisEvalExecutor(commands);
    }

    @Bean
    @ConditionalOnMissingBean
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
    @ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebFilter rateLimitingWebFilter(final ReactiveRateLimiter reactiveRateLimiter,
                                           final KeyResolver keyResolver) {
        return new RateLimiterWebFilter(reactiveRateLimiter, keyResolver);
    }
}
