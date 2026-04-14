package com.sfialok.ratelimiter.resolver;

import jakarta.servlet.http.HttpServletRequest;

public interface KeyResolver {
    String resolve(HttpServletRequest httpServletRequest);
}
