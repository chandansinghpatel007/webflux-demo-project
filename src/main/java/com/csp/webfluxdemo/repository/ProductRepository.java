package com.csp.webfluxdemo.repository;

import com.csp.webfluxdemo.model.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Spring Data R2DBC repository. Same shape as Spring Data JPA, except every method
 * returns Mono/Flux instead of the entity/List directly - that reactive return type
 * is the whole contract change.
 */
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    Flux<Product> findByNameContainingIgnoreCase(String name);
}
