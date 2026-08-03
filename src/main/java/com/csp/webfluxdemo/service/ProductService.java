package com.csp.webfluxdemo.service;

import com.csp.webfluxdemo.exception.ProductNotFoundException;
import com.csp.webfluxdemo.model.Product;
import com.csp.webfluxdemo.repository.ProductRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Business logic layer. Intentionally has no knowledge of HTTP - it just composes
 * Mono/Flux pipelines. This is what makes it reusable from BOTH the annotated
 * controller and the functional handler in this project.
 */
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Flux<Product> findAll() {
        return repository.findAll();
    }

    public Mono<Product> findById(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)));
    }

    public Flux<Product> search(String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

    public Mono<Product> create(Product product) {
        product.setId(null); // ensure the DB assigns a fresh id (insert, not update)
        return repository.save(product);
    }

    public Mono<Product> update(Long id, Product updated) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .flatMap(existing -> {
                    existing.setName(updated.getName());
                    existing.setPrice(updated.getPrice());
                    return repository.save(existing);
                });
    }

    public Mono<Void> delete(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .flatMap(repository::delete);
    }
}
