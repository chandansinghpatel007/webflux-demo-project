# webflux-demo

A minimal but complete Spring WebFlux example: a reactive `Product` CRUD API
backed by R2DBC (in-memory H2, zero setup needed), shown in **both** the
annotated-controller style and the functional-endpoint style, plus a
`WebClient` example that calls an external API. Pairs with `NOTES.md`.

## Requirements

- Java 17+
- Maven 3.9+ (or use your IDE's built-in Maven)

No database installation needed — H2 runs in-memory and is seeded on startup
from `src/main/resources/schema.sql`.

## Run it

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

## Try it out

Annotated-controller style (`ProductController`):

```bash
curl http://localhost:8080/api/products
curl http://localhost:8080/api/products/1
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Desk Lamp","price":24.99}'
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Mechanical Keyboard (Renamed)","price":94.99}'
curl -X DELETE http://localhost:8080/api/products/1
curl "http://localhost:8080/api/products?name=mouse"
```

The exact same operations via the functional-endpoint style
(`ProductRouter` + `ProductHandler`), reusing the identical `ProductService`:

```bash
curl http://localhost:8080/functional/products
curl http://localhost:8080/functional/products/2
```

WebClient example (calls a public quotes API reactively):

```bash
curl http://localhost:8080/api/quote
```

## Run the tests

```bash
mvn test
```

Includes:
- `ProductServiceTest` — unit tests using `StepVerifier` against a mocked repository.
- `ProductEndpointsIntegrationTest` — full-stack tests using `WebTestClient` against
  the real (in-memory) database, covering both endpoint styles.

## Where to start reading the code

1. `NOTES.md` — the concepts, read this first.
2. `model/Product.java` — the entity.
3. `repository/ProductRepository.java` — reactive Spring Data repository.
4. `service/ProductService.java` — business logic, returns `Mono`/`Flux`.
5. `controller/ProductController.java` — annotated REST controller.
6. `router/ProductRouter.java` + `handler/ProductHandler.java` — functional
   routing alternative to #5.
7. `client/QuoteClient.java` — `WebClient` calling an external service.
8. `exception/` — error → HTTP status mapping.
9. `src/test/...` — how to test each layer.
