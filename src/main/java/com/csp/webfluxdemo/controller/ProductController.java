package com.csp.webfluxdemo.controller;

import com.csp.webfluxdemo.model.Product;
import com.csp.webfluxdemo.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * "Annotated controller" style - looks almost identical to Spring MVC. The only
 * difference: methods return Mono<T>/Flux<T> instead of T. Spring subscribes to
 * these for you when writing the response.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<Product> getAll(@RequestParam(required = false) String name) {
        return (name == null || name.isBlank()) ? service.findAll() : service.search(name);
    }

    @GetMapping("/{id}")
    public Mono<Product> getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> create(@Valid @RequestBody Product product) {
        return service.create(product);
    }

    @PutMapping("/{id}")
    public Mono<Product> update(@PathVariable Long id, @Valid @RequestBody Product product) {
        return service.update(id, product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable Long id) {
        return service.delete(id);
    }
}
