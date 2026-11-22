# Coupons

REST service for creating and redeeming discount coupons.

## Database schema

![Database schema](docs/images/db_schema.png)

[docs/database-schema.md](docs/database-schema.md) describes the tables, columns and constraints.

## Development

Requires JDK 25 and Docker.

### Run in IntelliJ

In **Run → Edit Configurations… → CouponsApplication**, set **Active profiles** to `dev`, then run `CouponsApplication`.
Postgres starts automatically in Docker from `compose.yaml`.

### Run from the command line

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### API documentation

Swagger UI is available at http://localhost:8080/swagger-ui.html while the application runs.
The OpenAPI document is available at http://localhost:8080/v3/api-docs.
The `prod` profile turns both off.

### Conventions

[docs/conventions.md](docs/conventions.md) describes how the project documents the API with OpenAPI, how it uses mappers and how tests are written and organized.

### Useful commands

Run unit tests:

```bash
./mvnw test
```

Run unit and integration tests (classes ending with `IT`):

```bash
./mvnw verify
```

Remove the Postgres container together with its data:

```bash
docker compose down -v
```
