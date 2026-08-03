package com.csp.webfluxdemo.handler;

import com.csp.webfluxdemo.model.Product;
import com.csp.webfluxdemo.service.ProductService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * "Functional endpoint" style - the same operations as ProductController, but
 * expressed as plain methods wired up explicitly in ProductRouter instead of via
 * annotations. Notice it reuses the exact same ProductService: the routing style
 * you pick doesn't change your business logic layer at all.
 */
@Component
public class ProductHandler {

    private final ProductService service;

    public ProductHandler(ProductService service) {
        this.service = service;
    }

    public Mono<ServerResponse> getAll(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        var products = (name == null || name.isBlank()) ? service.findAll() : service.search(name);
        return ServerResponse.ok().body(products, Product.class);
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return service.findById(id)
                .flatMap(product -> ServerResponse.ok().bodyValue(product));
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(Product.class)
                .flatMap(service::create)
                .flatMap(saved -> ServerResponse.status(201).bodyValue(saved));
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return request.bodyToMono(Product.class)
                .flatMap(body -> service.update(id, body))
                .flatMap(saved -> ServerResponse.ok().bodyValue(saved));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return service.delete(id)
                .then(ServerResponse.noContent().build());
    }
}
