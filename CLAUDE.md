# CLAUDE.md

**YOU MUST READ [docs/conventions.md](docs/conventions.md) BEFORE YOU DO ANYTHING IN THIS PROJECT.**

You are to do exactly what you are told to do.
This project is a recruitment task written to production standards. The task description is in `task.md`.
`application-dev.yaml` holds config for a developer's laptop only (profile `dev`). Keep dev-only settings out of `application.yaml`.
Postgres in `compose.yaml` uses host port 5433
Follow the conventions for OpenAPI documentation, naming, exceptions, logging, mapping and tests described in [docs/conventions.md](docs/conventions.md).
After every change, check whether it needs new tests or changes to existing tests, and write them in the same change.

## Database schema

Liquibase migrations in `src/main/resources/db/changelog/changes/` define the schema.

- `users`: `id` UUID PK, `version` BIGINT, `first_name`, `last_name`.
- `coupon`: `id` UUID PK, `code` UNIQUE (stored uppercase), `country_code`, `max_uses`, `current_uses`, `created_at` TIMESTAMPTZ. `CHECK (max_uses > 0)`, `CHECK (current_uses >= 0 AND current_uses <= max_uses)`.
- `coupon_usage`: `id` UUID PK, `coupon_id` FK → `coupon`, `user_id` FK → `users`, `used_at` TIMESTAMPTZ. `UNIQUE (coupon_id, user_id)`.

**READ [docs/conventions.md](docs/conventions.md) IN FULL BEFORE YOU WRITE OR CHANGE ANY CODE, TEST OR DOCUMENT IN THIS PROJECT.**
