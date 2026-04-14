package com.sfialok.ratelimiter.filter;


import com.sfialok.ratelimiter.core.RateLimitDetails;
import com.sfialok.ratelimiter.core.RateLimiter;
import com.sfialok.ratelimiter.resolver.KeyResolver;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RateLimiterFilter implements Filter {

    final RateLimiter rateLimiter;
    final KeyResolver keyResolver;

    public RateLimiterFilter(final RateLimiter rateLimiter, final KeyResolver keyResolver) {
        this.rateLimiter = rateLimiter;
        this.keyResolver = keyResolver;
    }


    @Override
    public void doFilter(
            final ServletRequest servletRequest,
            final ServletResponse servletResponse,
            final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        final String key = keyResolver.resolve(httpRequest);
        final RateLimitDetails rateLimitDetails = rateLimiter.tryRateLimit(key);
        if (rateLimitDetails.allowed()) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            // No tokens left, return 429 Too Many Requests
            final Instant instant = Instant.ofEpochMilli(rateLimitDetails.resetAtEpochMs());
            final ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            final String formattedDateTime = dateTime.format(formatter);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("text/plain");
            httpResponse.getWriter().write("Too many requests. Please try again later.");
            httpResponse.addHeader("Retry-After", String.valueOf(rateLimitDetails.retryAfterMs()/1000));
            httpResponse.addHeader("X-RateLimit-Limit", String.valueOf(rateLimiter.getLimit()));
            httpResponse.addHeader("X-RateLimit-Remaining", String.valueOf(rateLimitDetails.requestsRemaining()));
            httpResponse.addHeader("X-RateLimit-Reset", formattedDateTime);
        }

    }
}
