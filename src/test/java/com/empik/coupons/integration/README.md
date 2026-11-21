# Tests

## Liquibase migrations in integration tests

Liquibase runs the migrations every time Spring creates a new context.

Each integration test class extends `PostgresIntegrationTestBase`.  
`PostgresIntegrationTestBase` starts a new Postgres container for each test class.  
`@DirtiesContext` on `PostgresIntegrationTestBase` makes Spring create a new context for each test class.  
So the migrations run once per test class.  
The migrations always run on an empty database.

## Data cleanup between test methods

All test methods in one test class share one Postgres container.  
`PostgresIntegrationTestBase` calls `DatabaseCleaner.cleanCouponTables()` before each test method.  
`DatabaseCleaner.cleanCouponTables()` deletes all rows from the `coupon_usage` and `coupon` tables.  
`DatabaseCleaner.cleanCouponTables()` keeps the rows in the `users` table.  
The Liquibase migration `0004-insert-users.sql` inserts a test user into the `users` table for the dev profile.
