package com.sfialok.ratelimiter.reactive.resolver;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface KeyResolver {
    Mono<String> resolve(ServerWebExchange exchange);
}
