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
| `integration.api`         | REST endpoints. A request goes through all layers down to Postgres.           |
| `integration.liquibase`   | Liquibase migrations, contexts and rollbacks.                                 |
| `integration.config`      | Shared setup for integration tests: `PostgresIntegrationTestBase` and `DatabaseCleaner`. |

### Unit and integration tests

A unit test class name ends with `Test`.
Unit tests run without Spring and without a database.
`./mvnw test` runs them.

An integration test class name ends with `IT`.
Integration tests run against Postgres in a Testcontainers container.
`./mvnw verify` runs them together with the unit tests.
[src/test/java/com/empik/coupons/integration/README.md](../src/test/java/com/empik/coupons/integration/README.md) describes how the containers and data cleanup work.

An API test extends `PostgresIntegrationTestBase` and has `@AutoConfigureMockMvc`.
It sends requests through `MockMvcTester`, so no HTTP server starts.

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
