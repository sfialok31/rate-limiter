### Usage Example
```java
RedisURI uri = RedisURI.Builder
                .redis("localhost", 6379)
                .build();

        RedisClient client = RedisClient.create(uri);
        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> commands = connection.sync();

        final RedisTokenBucketRateLimiter limiter =
            new RedisTokenBucketRateLimiter(
                    10,
                    1,
                    1000,
                    Clock.systemDefaultZone(),
                    new LettuceRedisEvalExecutor(commands)
            );
        // Safe to call from multiple threads
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 20; i++) {
            final int id = i;
            executor.submit(() -> {
                final RateLimitDetails result = limiter.tryRateLimit("user:1");
                System.out.printf("thread %d: result: %s\n", id, result.toString());
            });
        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        connection.close();
        client.shutdown();
```