# Conventions

## OpenAPI documentation

springdoc-openapi generates the OpenAPI document at `/v3/api-docs` and serves Swagger UI at `/swagger-ui.html`.
`application-prod.yaml` turns both off, so the `prod` profile serves no API documentation.
`OpenApiConfig` sets the title, version and description of the API.

Each controller implements an interface named `<Resource>Api`, for example `CouponController` implements `CouponApi`.
The interface holds only the OpenAPI annotations: `@Tag`, `@Operation` and one `@ApiResponse` for every possible status code.
The controller holds the Spring MVC annotations, for example `@PostMapping`, `@Valid` and `@RequestBody`.
springdoc reads the OpenAPI annotations from the interface methods.

Every field of a request or response DTO has `@Schema` with a `description` and an `example`.
springdoc adds the Bean Validation rules, for example `@Size` and `@Pattern`, to the schema itself.
A custom constraint such as `@IsoCountryCode` is invisible to springdoc, so `@Schema` describes it.

Error responses use the media type `application/problem+json` and the schema `ProblemDetail`.

## Naming

A method name states exactly what the method does and which object it works on, for example `loadDbIpLiteDatabaseIntoMemory(Path dbIpLiteFile)` instead of `load(Path file)`.
A parameter name states which object it holds in the same way.

## Exceptions

Custom exceptions live in `com.empik.coupons.exceptions` and extend `RuntimeException`.
Every custom exception has its own `@ExceptionHandler` method in `GlobalExceptionHandler`.
The handler returns a `ProblemDetail` with a title, a detail and the request details from `addRequestDetails`.

## Logging

Services log every business event at INFO, for example a created coupon, a used coupon and a rejected use with its reason.
A log line names the coupon code and the user id, never the client IP address, because the IP address is personal data.
The `prod` profile writes logs as JSON in the Elastic Common Schema (`logging.structured.format.console: ecs`).

## Mapping

MapStruct mappers in `com.empik.coupons.mappers` convert between DTOs and entities.
Services build entities from DTOs by calling a mapper method, for example `couponMapper.toEntity(request, Instant.now())`.
Controllers build response DTOs from entities by calling a mapper method, for example `couponMapper.toResponse(coupon)`.

The build sets two MapStruct options in `pom.xml`:

- `mapstruct.defaultComponentModel=spring` makes every generated mapper a Spring bean.
- `mapstruct.unmappedTargetPolicy=ERROR` fails the build when a target field has no mapping.

## Tests

### Packages

Tests live in `src/test/java/com/empik/coupons/`:

| Package                   | What it tests                                                                 |
|---------------------------|-------------------------------------------------------------------------------|
| `unit.mappers`            | MapStruct mappers. Each mapper method has a unit test.                        |
| `unit.services`           | Services. Mockito mocks the repositories.                                     |
| `unit.validation`         | Custom Bean Validation constraints, for example `@IsoCountryCode`.            |
| `unit.dtos`               | Boundary values of the Bean Validation constraints on request DTOs, checked with `jakarta.validation.Validator`. |
| `unit.exceptions`         | Handler methods of `GlobalExceptionHandler`, called directly with `MockHttpServletRequest`. |
| `unit.geoip`              | Country lookup and download of the DB-IP Lite file. A local HTTP server plays the DB-IP server. |
| `integration.api`         | REST endpoints. A request goes through all layers down to Postgres.           |
| `integration.repositories` | Custom repository queries, for example the conditional `UPDATE` of `current_uses`. |
| `integration.liquibase`   | Liquibase migrations, contexts and rollbacks.                                 |
| `integration.liquibase.config` | Shared setup for integration tests: `PostgresIntegrationTestBase` and `DatabaseCleaner`. |

### Unit and integration tests

A unit test class name ends with `Test`.
Unit tests run without Spring and without a database.
`./mvnw test` runs them.

An integration test class name ends with `IT`.
Integration tests run against Postgres in a Testcontainers container.
`./mvnw verify` runs them together with the unit tests.
[src/test/java/com/empik/coupons/integration/liquibase/README.md](../src/test/java/com/empik/coupons/integration/liquibase/README.md) describes how the containers and data cleanup work.

An integration test runs with the `test` profile, so Liquibase creates only the tables and the test inserts its own data.
An API test extends `PostgresIntegrationTestBase` and has `@AutoConfigureMockMvc`.
It sends requests through `MockMvcTester`, so no HTTP server starts.

A repository test has `@DataJpaTest` and `@AutoConfigureTestDatabase(replace = NONE)` and starts its own Postgres container with `@ServiceConnection` and has `@ActiveProfiles("test")`.
`@DataJpaTest` starts only JPA, the repositories and Liquibase, and rolls back each test method.

### Test method structure

A test method name starts with `given<Condition>_` and then describes the expected result, for example `givenValidRequest_shouldCreateCoupon`.

The body of a test method has three sections marked with comments:

```java
// given
CreateCouponRequest request = new CreateCouponRequest(
        "wiosna",
        "pl",
        10
);

// when
Coupon coupon = mapper.toEntity(
        request,
        CREATED_AT
);

// then
assertThat(coupon.getCode()).isEqualTo("WIOSNA");
```

A test without setup code has no `// given` section.
A test that expects an exception catches it with `catchThrowable` in the `// when` section and checks it in the `// then` section.
