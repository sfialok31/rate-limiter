package com.sfialok.ratelimiter.resolver;

import com.sfialok.ratelimiter.resolver.KeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class IPKeyResolver implements KeyResolver {
    @Override
    public String resolve(final HttpServletRequest httpServletRequest) {
        return httpServletRequest.getRemoteAddr();
    }
}
