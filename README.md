# Distributed Rate Limiter

A distributed rate-limiting library for Java applications using Redis and the Token Bucket algorithm. Provides Spring Boot starters for easy integration with both reactive (WebFlux) and non-reactive (Spring MVC) applications.

## Features

- **Token Bucket Algorithm**: Allows controlled bursts while enforcing a steady average rate
- **Distributed**: Uses Redis for shared state across multiple application instances
- **Atomic Operations**: Lua scripts ensure thread-safe rate limiting
- **Fail-Open**: Allows requests if Redis is unreachable
- **Request Coalescing**: Prevents thundering herd when reloading Lua scripts
- **Spring Boot Integration**: Auto-configuration for both reactive and blocking applications

## Modules

| Module | Description |
|--------|-------------|
| `rate-limiter-core` | Core interfaces and models |
| `rate-limiter-redis` | Redis implementation (reactive and blocking) |
| `rate-limiter-spring-boot-starter` | Spring MVC integration |
| `reactive-rate-limiter-spring-boot-starter` | Spring WebFlux integration |

## Requirements

- Java 21+
- Redis
- Spring Boot 3.x

## Installation

Add the appropriate starter to your `build.gradle`:

```gradle
// For Spring WebFlux (reactive)
implementation 'com.sfialok.ratelimiter:reactive-rate-limiter-spring-boot-starter:1.0.0'

// For Spring MVC (blocking)
implementation 'com.sfialok.ratelimiter:rate-limiter-spring-boot-starter:1.0.0'
```

## Configuration

Configure the rate limiter in your `application.properties`:

```properties
# For reactive (WebFlux)
rate-limiter-reactive.enabled=true
rate-limiter-reactive.capacity=100
rate-limiter-reactive.refill-rate=10
rate-limiter-reactive.refill-interval-ms=1000

# For blocking (Spring MVC)
rate-limiter.enabled=true
rate-limiter.capacity=100
rate-limiter.refill-rate=10
rate-limiter.refill-interval-ms=1000
```

### Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `enabled` | Enable/disable rate limiting | `true` |
| `capacity` | Maximum tokens in the bucket | `100` |
| `refill-rate` | Tokens added per refill interval | `10` |
| `refill-interval-ms` | Refill interval in milliseconds | `1000` |

## HTTP Response Headers

When rate limiting is applied, the following headers are included:

| Header | Description |
|--------|-------------|
| `X-RateLimit-Limit` | Maximum requests allowed |
| `X-RateLimit-Remaining` | Remaining requests in current window |
| `X-RateLimit-Reset` | Timestamp when the limit resets |
| `Retry-After` | Seconds until requests are allowed (when limited) |

## Example

```bash
# Make requests to a rate-limited endpoint
for i in {1..10}; do curl -i http://localhost:8080/product/12; done
```

Successful response:
```
HTTP/1.1 200 OK
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 4
```

Rate-limited response:
```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 0
Retry-After: 9
```

## Building

```bash
./gradlew build
```

## Running the Demo

Start Redis, then run the demo application:

```bash
cd demo-spring-app
./gradlew bootRun
```

## License

MIT License - see [LICENSE](LICENSE) for details.