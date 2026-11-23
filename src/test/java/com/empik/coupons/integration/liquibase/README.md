# Tests

## Liquibase migrations in integration tests

Liquibase runs the migrations every time Spring creates a new context.

The API tests in `integration.api` extend `PostgresIntegrationTestBase`.  
`PostgresIntegrationTestBase` starts a new Postgres container for each test class.  
`@DirtiesContext` on `PostgresIntegrationTestBase` makes Spring create a new context for each test class.  
So the migrations run once per test class.  
The migrations always run on an empty database.

`PostgresIntegrationTestBase` activates the `test` profile.  
`src/test/resources/application-test.yaml` sets the Liquibase context to `test`.  
Only the DDL changesets have the `test` context, so the tests start with empty tables and create their own data.  
The DML changesets `0004-insert-users.sql` and `0005-insert-user-anna-nowak.sql` have only the `dev` context, so they never run in these tests.  
`LiquibaseContextIT` does not extend `PostgresIntegrationTestBase`, because it checks the data that the `dev` changesets insert.

## Data cleanup between test methods

All test methods in one test class share one Postgres container.  
`PostgresIntegrationTestBase` calls `DatabaseCleaner.deleteAllCouponUsagesCouponsAndUsers()` before each test method.  
This method deletes all rows from the `coupon_usage`, `coupon` and `users` tables.
