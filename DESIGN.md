# Distributed Rate Limiter — Design Document

## 1. Problem Statement

Design a distributed rate-limiter for API requests.

---

## 2. Requirements

### Functional
- Rate limit users based on source IP address (no authentication handling).
- Provide helpful information so the user knows rate-limiting details:
  - Number of requests remaining
  - Limit reset after (duration)
  - Limit reset at (timestamp)
- Configurable token bucket algorithm parameters like capactity (C),  refill rate (T/S) where T = tokens to refill, S = refill interval

### Non-Functional
- Highly available rate-limiter.
- Adds very low latency per request.

---

## 3. API Design

---

RateLimitResult
- ip
- requests_remaining
- is_rate_limited
- reset_after
- reset_at

`RateLimitResult checkRateLimit(ip)`

HTTP API
If rate limited:
- Response code: 429 Too Many Requests
- Response headers: Retry-After, X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset
Else:
- Standard HTTP Response

## 4. Algorithm Choice

### Token Bucket

A token bucket rate limiter consists of a bucket of fixed capactity in which tokens are refilled at regular intervals.
A request consumes a token from the bucket. If no token is present in the bucket, request is rate-limited.
It allows controlled bursts of traffic while enforcing a steady average rate.
tokens are refilled in the bucket at regular intervals.

---

## 5. Data Storage

### Redis

Externalized State which can be accessed by all rate-limiter replicas and provides sub-ms latency.

redis key - "bucket_${ip}"
redis value - {"tokens", "last_refill_time"}
expiry - (capacity*refill_interval)/refill_tokens + 1

---

## 6. How to Handle Concurrency?

Use Lua Script for atomicity.
Redis is single-threaded, so no other redis commands can interleave with the lua script.

--- 

## 7. Failure Handling

- Multiple replicas of rate-limiter run. If one goes down, other replica continues handling requests.
- If redis is unreachable, we fail-open so requests are allowed in that time.

--- 

## 8. Language/Framework

- Java
- Spring Boot
- Redis java client - jedis

## 9. Deployment

| Approach | Pros | Cons |
|---|---|---|
| API Gateway | Centralized | Less flexible |
| Dedicated Service | Reusable | Extra latency |
| Library | Low latency | Harder to update |

For this project, we implement it as a **library** since it provides low latency, and is easy to integrate into multiple services while providing distributed rate limiting via redis.

## 10. Limitations

- Only uses one algorithm - token bucket, no support for other algorithms.
- Only handles rate-limiting users based on their ip. This can be weak (NAT, shared IPs)
