# Coupons

REST service for creating and redeeming discount coupons.

## Database schema

![Database schema](docs/images/db_schema.png)

[docs/database-schema.md](docs/database-schema.md) describes the tables, columns and constraints.

## IP geolocation

The service finds the country of an IP address in the DB-IP Lite database file `geoip/dbip-country-lite-YYYY-MM.mmdb`.
The repository contains the file, so the service works right after cloning.
DB-IP publishes a new file every month, usually on the 1st.
Every day at 04:00 UTC and at startup, the service downloads the file of the current month when it is missing and deletes the older file.
The `coupons.geoip` properties in `application.yaml` configure the directory and the download.

[IP Geolocation by DB-IP](https://db-ip.com) is licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

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

### Health checks

`/actuator/health/liveness` reports whether the application is running.
`/actuator/health/readiness` reports whether the application can serve requests. It reports DOWN while no DB-IP Lite file is loaded.
Kubernetes can use them as the liveness and readiness probes.

### Conventions

[docs/conventions.md](docs/conventions.md) describes how the project documents the API with OpenAPI, names methods, handles exceptions, logs, uses mappers and writes and organizes tests.

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
