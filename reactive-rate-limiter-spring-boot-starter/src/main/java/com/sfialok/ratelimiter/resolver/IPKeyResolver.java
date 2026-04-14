package com.sfialok.ratelimiter.resolver;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class IPKeyResolver implements KeyResolver {
    @Override
    public Mono<String> resolve(final ServerWebExchange exchange) {
        return Mono.just(Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress());
    }
}
