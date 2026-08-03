package com.csp.webfluxdemo.router;

import com.csp.webfluxdemo.handler.ProductHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Wires URLs -> handler methods explicitly, as an alternative to @RequestMapping
 * annotations. Mounted under /functional/products so you can compare it side by
 * side with the annotated version at /api/products - both call the same
 * ProductService underneath.
 */
@Configuration
public class ProductRouter {

    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductHandler handler) {
        return RouterFunctions.route()
                .path("/functional/products", builder -> builder
                        .GET("", handler::getAll)
                        .POST("", handler::create)
                        .GET("/{id}", handler::getById)
                        .PUT("/{id}", handler::update)
                        .DELETE("/{id}", handler::delete))
                .build();
    }
}
