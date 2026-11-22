package com.empik.coupons.integration.liquibase.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts a new Postgres container for each test class.
 * {@link DirtiesContext} closes the Spring context after each class,
 * because the cached context would point to the stopped container.
 * Before each test method, {@link DatabaseCleaner} deletes the data of the previous method.
 */
@SpringBootTest
@Testcontainers
@DirtiesContext
@Import(DatabaseCleaner.class)
public abstract class PostgresIntegrationTestBase {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@BeforeEach
	void cleanDatabase() {
		databaseCleaner.cleanCouponTables();
	}

}
