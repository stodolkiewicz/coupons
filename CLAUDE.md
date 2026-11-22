# CLAUDE.md

You are to do exactly what you are told to do.
This project is a recruitment task written to production standards. The task description is in `task.md`.
`application-dev.yaml` holds config for a developer's laptop only (profile `dev`). Keep dev-only settings out of `application.yaml`.
Postgres in `compose.yaml` uses host port 5433
Follow the conventions for mapping and tests described in [docs/conventions.md](docs/conventions.md).

## Database schema

Liquibase migrations in `src/main/resources/db/changelog/changes/` define the schema.

- `users`: `id` UUID PK, `version` BIGINT, `first_name`, `last_name`.
- `coupon`: `id` UUID PK, `code` UNIQUE (stored uppercase), `country_code`, `max_uses`, `current_uses`, `created_at` TIMESTAMPTZ. `CHECK (max_uses > 0)`, `CHECK (current_uses >= 0 AND current_uses <= max_uses)`.
- `coupon_usage`: `id` UUID PK, `coupon_id` FK → `coupon`, `user_id` FK → `users`, `used_at` TIMESTAMPTZ. `UNIQUE (coupon_id, user_id)`.

## Mapping

Convert between DTOs and entities with MapStruct mappers in `com.empik.coupons.mappers`.
Services build entities from DTOs by calling a mapper method, for example `couponMapper.toEntity(request, Instant.now())`.
Cover every mapper method with a unit test in `src/test/java/com/empik/coupons/unit/mappers/`.

## Tests

Split the body of every test method into sections marked with `// given`, `// when` and `// then` comments.
Write `// given` only when the test has setup code.
