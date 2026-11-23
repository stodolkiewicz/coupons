package com.empik.coupons.integration.liquibase;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Checks which Liquibase changesets each profile runs.
 * The test does not extend PostgresIntegrationTestBase,
 * because its DatabaseCleaner would delete the users that the dev changesets insert.
 */
class LiquibaseContextIT {

	@Nested
	@SpringBootTest(properties = "coupons.geoip.download.enabled=false")
	@ActiveProfiles("dev")
	@Testcontainers
	@DirtiesContext
	class DevProfile {

		@Container
		@ServiceConnection
		static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

		@Autowired
		UserRepository userRepository;

		@Test
		@DisplayName("The dev profile runs all DDL and DML changesets, so the test users are inserted")
		void givenDevProfile_liquibaseMigrationShouldExecuteDevContextChangesets() {
			// when
			long userCount = userRepository.count();

			// then
			assertThat(userCount).isGreaterThan(0);
		}

	}

	@Nested
	@SpringBootTest(properties = "coupons.geoip.download.enabled=false")
	@ActiveProfiles("prod")
	@Testcontainers
	@DirtiesContext
	class ProdProfile {

		@Container
		@ServiceConnection
		static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

		@Autowired
		UserRepository userRepository;

		@Test
		@DisplayName("The prod profile runs only DDL changesets, so no test user is inserted")
		void givenProdProfile_liquibaseMigrationShouldExecuteOnlyProdContextChangesets() {
			// when
			long userCount = userRepository.count();

			// then
			assertThat(userCount).isZero();
		}

	}

}
