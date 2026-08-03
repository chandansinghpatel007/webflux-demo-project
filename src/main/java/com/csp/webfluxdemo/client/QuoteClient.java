package com.csp.webfluxdemo.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class QuoteClient {

    private final WebClient webClient;

    public QuoteClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> fetchRandomQuote() {
        return webClient.get()
                .uri("https://dummyjson.com/quotes/random")
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> {
                    String quote = (String) body.get("quote");
                    String author = (String) body.get("author");
                    return (quote != null) ? "\"" + quote + "\" — " + author : "(no quote available)";
                })
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> System.err.println("WebClient Error: " + e.getMessage()))
                .onErrorReturn("Could not reach the quote service right now.");
    }
}