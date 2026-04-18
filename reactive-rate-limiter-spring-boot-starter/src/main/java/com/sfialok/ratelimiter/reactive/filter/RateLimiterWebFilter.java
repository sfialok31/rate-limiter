package com.sfialok.ratelimiter.reactive.filter;


import com.sfialok.ratelimiter.core.RateLimitDetails;
import com.sfialok.ratelimiter.redis.reactive.ReactiveRateLimiter;
import com.sfialok.ratelimiter.reactive.resolver.KeyResolver;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.SocketException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Log4j2
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
                .flatMap(key ->
                        reactiveRateLimiter.tryRateLimit(key)
                                .onErrorResume(ex -> {
                                    // fail-open in case of socket exception
                                    if (ExceptionUtils.hasCause(ex, SocketException.class)) {
                                        log.warn("Got Socket exception connecting with redis, failed open the request with key {}", key);
                                        return Mono.just(new RateLimitDetails(
                                                key,
                                                reactiveRateLimiter.getLimit(),
                                                true,
                                                -1,
                                                -1
                                        ));
                                    }
                                    return Mono.error(ex);
                                }))
                .flatMap(rateLimitDetails -> {
                    exchange.getResponse().getHeaders()
                            .add("X-RateLimit-Limit", String.valueOf(reactiveRateLimiter.getLimit()));
                    exchange.getResponse().getHeaders()
                            .add("X-RateLimit-Remaining", String.valueOf(rateLimitDetails.requestsRemaining()));
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
                            .add("X-RateLimit-Reset", formattedDateTime);
                    return exchange.getResponse().setComplete();
                });
    }
}
