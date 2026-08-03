package com.csp.webfluxdemo;

import com.csp.webfluxdemo.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test: starts the real Spring context (in-memory H2 via R2DBC,
 * seeded by schema.sql) and drives it through WebTestClient - no real server socket
 * needed. Exercises both the annotated controller (/api/products) and the
 * functional router (/functional/products) to prove they're equivalent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductEndpointsIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void annotatedEndpoint_getAll_returnsSeedData() {
        webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Product.class)
                .value(products -> assertThat(products).isNotEmpty());
    }

    @Test
    void annotatedEndpoint_create_thenGetById_roundTrips() {
        Product toCreate = Product.of("Test Widget", BigDecimal.valueOf(12.34));

        Product created = webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toCreate)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Product.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();

        webTestClient.get().uri("/api/products/{id}", created.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Test Widget");
    }

    @Test
    void annotatedEndpoint_getMissing_returns404() {
        webTestClient.get().uri("/api/products/{id}", 999999)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void functionalEndpoint_getAll_returnsSeedData() {
        webTestClient.get().uri("/functional/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Product.class)
                .value(products -> assertThat(products).isNotEmpty());
    }

    @Test
    void functionalEndpoint_create_thenDelete_works() {
        Product toCreate = Product.of("Functional Widget", BigDecimal.valueOf(5.00));

        Product created = webTestClient.post().uri("/functional/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toCreate)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Product.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();

        webTestClient.delete().uri("/functional/products/{id}", created.getId())
                .exchange()
                .expectStatus().isNoContent();
    }
}