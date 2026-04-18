package com.sfialok.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalTime;

@RestController
public class ProductController {
    @GetMapping("/product/{id}")
    public Mono<String> getProduct(final @PathVariable("id") String id) {
        return Mono.just("Product with id: " + id);
    }

    @GetMapping("/products")
    public Flux<String> getProducts() {
        return Flux.just("Product 1", "Product 2", "Product 3").delayElements(Duration.ofSeconds(1));
    }

    @GetMapping(value = "/product/stream", produces = "text/event-stream")
    public Flux<String> streamData() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(tick -> "product: " + LocalTime.now());
    }
}