package com.csp.webfluxdemo.controller;

import com.csp.webfluxdemo.client.QuoteClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class QuoteController {

    private final QuoteClient quoteClient;

    public QuoteController(QuoteClient quoteClient) {
        this.quoteClient = quoteClient;
    }

    @GetMapping("/api/quote")
    public Mono<String> getQuote() {
        return quoteClient.fetchRandomQuote();
    }
}
