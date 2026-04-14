package com.sfialok.ratelimiter.filter;


import com.sfialok.ratelimiter.redis.reactive.ReactiveRateLimiter;
import com.sfialok.ratelimiter.resolver.KeyResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RateLimiterWebFilter implements WebFilter {

    final ReactiveRateLimiter reactiveRateLimiter;
    final KeyResolver keyResolver;

    public RateLimiterWebFilter(final ReactiveRateLimiter reactiveRateLimiter, final KeyResolver keyResolver) {
        this.reactiveRateLimiter = reactiveRateLimiter;
        this.keyResolver = keyResolver;
    }


    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        return keyResolver.resolve(exchange)
                .flatMap(key -> reactiveRateLimiter.tryRateLimit(key))
                .flatMap(rateLimitDetails -> {
                    if (rateLimitDetails.allowed()) {
                        return chain.filter(exchange);
                    }
                    final Instant instant = Instant.ofEpochMilli(rateLimitDetails.resetAtEpochMs());
                    final ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());
                    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    final String formattedDateTime = dateTime.format(formatter);
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders()
                            .add("Retry-After", String.valueOf(rateLimitDetails.retryAfterMs()/1000));
                    exchange.getResponse().getHeaders()
                            .add("X-RateLimit-Limit", String.valueOf(reactiveRateLimiter.getLimit()));
                    exchange.getResponse().getHeaders()
                            .add("X-RateLimit-Remaining", String.valueOf(rateLimitDetails.requestsRemaining()));
                    exchange.getResponse().getHeaders()
                            .add("X-RateLimit-Reset", formattedDateTime);
                    return exchange.getResponse().setComplete();
                });
    }
}
