package com.csp.webfluxdemo;

import com.csp.webfluxdemo.exception.ProductNotFoundException;
import com.csp.webfluxdemo.model.Product;
import com.csp.webfluxdemo.repository.ProductRepository;
import com.csp.webfluxdemo.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pure unit test for the service layer, with the repository mocked. Shows the
 * core testing tool for reactive code: StepVerifier, which subscribes to a
 * Mono/Flux and lets you assert on the emitted signals step by step.
 */
class ProductServiceTest {

    private final ProductRepository repository = Mockito.mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository);

    @Test
    void findByIdReturnsProductWhenPresent() {
        Product keyboard = new Product(1L, "Keyboard", BigDecimal.valueOf(50));
        when(repository.findById(1L)).thenReturn(Mono.just(keyboard));

        StepVerifier.create(service.findById(1L))
                .expectNextMatches(p -> p.getName().equals("Keyboard"))
                .verifyComplete();
    }

    @Test
    void findByIdErrorsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(99L))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void findAllReturnsEveryProduct() {
        when(repository.findAll()).thenReturn(Flux.just(
                new Product(1L, "Keyboard", BigDecimal.valueOf(50)),
                new Product(2L, "Mouse", BigDecimal.valueOf(20))
        ));

        StepVerifier.create(service.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void createSavesAndReturnsNewProduct() {
        Product toSave = Product.of("Monitor", BigDecimal.valueOf(300));
        Product saved = new Product(5L, "Monitor", BigDecimal.valueOf(300));
        when(repository.save(any(Product.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(service.create(toSave))
                .expectNextMatches(p -> p.getId().equals(5L))
                .verifyComplete();
    }
}
