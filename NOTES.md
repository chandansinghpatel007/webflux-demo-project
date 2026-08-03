# Spring WebFlux — Beginner's Notes

A from-scratch guide to reactive programming with Spring, paired with the runnable
project in this repo (`src/`). Read top to bottom the first time; use it as a
reference afterward.

---

## 1. Why does WebFlux exist?

Classic Spring MVC is **blocking, thread-per-request**. Every incoming HTTP
request is handed a thread from a pool, and that thread sits idle (blocked)
whenever it waits on I/O — a database call, a downstream HTTP call, etc. Under
high concurrency (thousands of slow, I/O-bound connections) you eventually run
out of threads, no matter how large the pool.

Spring WebFlux is **non-blocking and event-loop based**, built on
[Project Reactor](https://projectreactor.io/) and, by default, the
[Netty](https://netty.io/) server. A small, fixed number of threads (usually
one per CPU core) handle many concurrent requests by never blocking — when a
task needs to wait on I/O, it registers a callback and the thread moves on to
other work. When the I/O completes, the callback resumes the work.

**Rule of thumb:**
- Use **Spring MVC** for typical CRUD apps, low-to-medium concurrency, or when
  your team/libraries are blocking (most JDBC drivers, for instance).
- Use **WebFlux** when you have high-concurrency, I/O-heavy workloads
  (many slow downstream calls, streaming data, WebSockets/SSE, or you're
  already in a reactive stack end-to-end — reactive DB driver, reactive
  clients, etc.).

**The #1 mistake:** using WebFlux but calling blocking code (JDBC, blocking
HTTP clients, `Thread.sleep`) inside the reactive pipeline. That defeats the
entire purpose and can stall the event loop for *all* requests, not just one.
If any part of your stack is blocking, you gain little from WebFlux and MVC is
usually the better/simpler choice.

---

## 2. Reactive Streams in one paragraph

Reactive Streams is a specification (`Publisher`, `Subscriber`, `Subscription`,
`Processor`) for asynchronous stream processing with non-blocking
**backpressure** — the ability for a consumer to say "send me only N items at
a time" so a fast producer can't overwhelm a slow consumer. Project Reactor is
Spring's implementation of this spec, and it gives you two main types:

| Type | Represents | Analogy |
|---|---|---|
| `Mono<T>` | 0 or 1 element | `Optional<T>` / `CompletableFuture<T>`, but lazy & composable |
| `Flux<T>` | 0 to N elements (possibly infinite) | `Stream<T>` / `List<T>`, but lazy, async & composable |

**Critical concept: nothing happens until you subscribe.** `Mono` and `Flux`
are *descriptions* of a computation, not the result. Building a chain of
operators (`.map()`, `.filter()`, etc.) does no work — it just builds a
pipeline. Work starts only when something subscribes (Spring WebFlux
subscribes for you when it writes the HTTP response; in tests, `StepVerifier`
subscribes; in your own code, `.subscribe()` does).

```java
Mono<String> mono = Mono.just("hello")
        .map(String::toUpperCase); // nothing executed yet!

mono.subscribe(System.out::println); // NOW it runs -> prints "HELLO"
```

---

## 3. Core Mono/Flux operators cheat sheet

```java
// Creation
Mono.just(value)
Mono.empty()
Mono.error(new RuntimeException("boom"))
Flux.just(1, 2, 3)
Flux.fromIterable(list)
Flux.range(1, 10)
Mono.fromCallable(() -> blockingCall())        // wrap legacy blocking code
        .subscribeOn(Schedulers.boundedElastic()); // ...on a thread meant for blocking work

// Transformation
.map(x -> x * 2)              // sync, 1-to-1 transform
.flatMap(x -> callAnotherApi(x))  // async, 1-to-1-or-many, flattens nested Mono/Flux
.filter(x -> x > 10)
.flatMapMany(mono -> ...)     // Mono -> Flux

// Combining
Mono.zip(monoA, monoB)               // wait for both, combine results
Flux.merge(fluxA, fluxB)             // interleave as they arrive
Flux.concat(fluxA, fluxB)            // fluxA fully, then fluxB

// Error handling
.onErrorReturn(fallbackValue)
.onErrorResume(ex -> Mono.just(fallback))
.onErrorMap(ex -> new CustomException(ex))
.retry(3)
.timeout(Duration.ofSeconds(5))

// Side effects (don't change the data, just observe)
.doOnNext(x -> log.info("saw {}", x))
.doOnError(ex -> log.error("failed", ex))
.doOnSubscribe(s -> log.info("subscribed"))
.doFinally(signal -> log.info("done: {}", signal))

// Defaults / empty handling
.defaultIfEmpty(fallback)
.switchIfEmpty(Mono.error(new NotFoundException()))

// Blocking bridge (AVOID in production reactive code — mainly for tests/main methods)
.block()
.blockFirst()
```

**`map` vs `flatMap`** trips everyone up at first:
- `map(Function<T,R>)` — synchronous, you return a plain value `R`.
- `flatMap(Function<T, Mono<R>>)` — asynchronous, you return *another*
  reactive type, and Reactor "flattens" it so you don't end up with
  `Mono<Mono<R>>`. Use `flatMap` whenever the next step is itself reactive
  (a DB call, an HTTP call, etc.).

---

## 4. Two ways to define endpoints

### 4a. Annotated Controllers (looks just like Spring MVC)

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<Product> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> create(@RequestBody Product product) {
        return service.create(product);
    }
}
```

The key mental shift from MVC: **methods return `Mono<T>` / `Flux<T>`
instead of `T`**. Spring subscribes to them for you and streams the result
back as the HTTP response once data is available.

### 4b. Functional Endpoints (`RouterFunction` + `HandlerFunction`)

A more explicit, code-as-configuration style — popular for small
microservices or when you want fine control over routing:

```java
@Configuration
public class ProductRouter {

    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler handler) {
        return RouterFunctions.route()
                .GET("/functional/products", handler::getAll)
                .GET("/functional/products/{id}", handler::getById)
                .POST("/functional/products", handler::create)
                .DELETE("/functional/products/{id}", handler::delete)
                .build();
    }
}

@Component
public class ProductHandler {

    private final ProductService service;
    public ProductHandler(ProductService service) { this.service = service; }

    public Mono<ServerResponse> getAll(ServerRequest request) {
        return ServerResponse.ok().body(service.findAll(), Product.class);
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return service.findById(id)
                .flatMap(p -> ServerResponse.ok().bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}
```

Both styles are fully supported and can even coexist in the same app (the
example project does exactly this, so you can compare them side by side).

---

## 5. Reactive data access: R2DBC

JDBC is blocking by nature (the API itself blocks the calling thread), so it
can't be used inside a truly reactive pipeline without breaking non-blocking
guarantees. **R2DBC** ("Reactive Relational Database Connectivity") is the
non-blocking alternative, with Spring Data support via
`ReactiveCrudRepository`:

```java
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {
    Flux<Product> findByNameContainingIgnoreCase(String name);
}
```

Usage looks like Spring Data JPA, but every method returns `Mono`/`Flux`
instead of the entity or `List<T>` directly — that's the whole API surface
change. The example project uses R2DBC with an in-memory H2 database so you
can run it with zero setup.

---

## 6. Calling other services: `WebClient`

`WebClient` replaces the old blocking `RestTemplate` for reactive apps:

```java
WebClient client = WebClient.builder()
        .baseUrl("https://api.example.com")
        .build();

Mono<User> user = client.get()
        .uri("/users/{id}", id)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError,
                  resp -> Mono.error(new NotFoundException()))
        .bodyToMono(User.class)
        .timeout(Duration.ofSeconds(3))
        .retry(2);
```

Because it returns `Mono`/`Flux`, you can `flatMap` it directly into a larger
reactive chain — e.g., "look up a product, then call an external pricing
service, then combine the results" — without ever blocking a thread while
waiting on the network.

---

## 7. Error handling patterns

```java
service.findById(id)
    .switchIfEmpty(Mono.error(new ProductNotFoundException(id))) // empty -> error
    .onErrorResume(ProductNotFoundException.class,
                   ex -> Mono.just(Product.placeholder()))       // recover
    .onErrorMap(DataAccessException.class,
                DatabaseUnavailableException::new);              // translate
```

For a global HTTP-level mapping (turn exceptions into proper status codes),
implement `@ControllerAdvice` + `@ExceptionHandler` exactly as in Spring MVC —
it works unchanged, just have the handler methods return `Mono<ResponseEntity<...>>`
if needed. See `GlobalExceptionHandler` in the example project.

---

## 8. Testing

**`StepVerifier`** — the reactive equivalent of asserting on a value:

```java
@Test
void findByIdReturnsProduct() {
    StepVerifier.create(service.findById(1L))
            .expectNextMatches(p -> p.getName().equals("Keyboard"))
            .verifyComplete();
}
```

**`WebTestClient`** — integration-tests your endpoints without a real server
thread-per-request model, works with both annotated and functional endpoints:

```java
@Test
void getAllReturnsOk() {
    webTestClient.get().uri("/api/products")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Product.class);
}
```

---

## 9. Common pitfalls (read this twice)

1. **Blocking inside a reactive chain.** Never call `.block()`, JDBC, or
   `Thread.sleep()` inside a `map`/`flatMap` on the main event-loop threads.
   If you truly must call blocking code, wrap it in
   `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.
2. **Forgetting to subscribe.** If you build a `Mono`/`Flux` chain and never
   return it from a controller method or call `.subscribe()`, *nothing
   happens* — a very common "why isn't my code running" bug. (Spring
   subscribes automatically for values you return from `@RestController`
   methods — that's the one case you don't do it yourself.)
3. **Using `map` where you needed `flatMap`,** ending up with a
   `Mono<Mono<T>>` and confusing compiler errors.
4. **Sharing mutable state** across a reactive pipeline without a
   thread-safety story — different operators can execute on different
   threads.
5. **Overusing WebFlux.** If your database driver, ORM, or key dependency is
   blocking, you've just added reactive complexity without the benefit.
   Measure before you reach for it.

---

## 10. How the example project is organized

```
src/main/java/com/example/webfluxdemo/
├── WebfluxDemoApplication.java     # entry point
├── model/Product.java              # R2DBC entity
├── repository/ProductRepository.java
├── service/ProductService.java     # business logic, framework-agnostic
├── controller/ProductController.java  # annotated-style REST endpoints
├── router/ProductRouter.java       # functional-style routing config
├── handler/ProductHandler.java     # functional-style handlers
├── client/QuoteClient.java         # WebClient example calling a public API
└── exception/
    ├── ProductNotFoundException.java
    └── GlobalExceptionHandler.java
src/main/resources/
├── application.yml
└── schema.sql                      # creates the `product` table on startup
src/test/java/...                   # StepVerifier + WebTestClient tests
```

Run it (see `README.md`) and hit both `/api/products` (annotated) and
`/functional/products` (functional) to compare the two styles against the
exact same `ProductService`.

## 11. Where to go next

- Official reference docs: https://docs.spring.io/spring-framework/reference/web/webflux.html
- Project Reactor docs & the interactive "Lite Rx API Hands-on": https://projectreactor.io/learn
- `WebClient` reference: https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html
- Spring Data R2DBC: https://docs.spring.io/spring-data/r2dbc/reference/
