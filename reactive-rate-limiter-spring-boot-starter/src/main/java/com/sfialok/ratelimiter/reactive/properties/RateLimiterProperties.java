package com.sfialok.ratelimiter.reactive.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limiter-reactive")
@Data
public class RateLimiterProperties {
    private boolean enabled = true;

    private int capacity = 100;
    private int refillRate = 10;
    private int refillIntervalMs = 1000;
}
