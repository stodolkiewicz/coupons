package com.empik.coupons.integration.liquibase;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.integration.liquibase.config.PostgresIntegrationTestBase;
import com.empik.coupons.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

class LiquibaseContextIT {

	@Nested
	@ActiveProfiles("dev")
	class DevProfile extends PostgresIntegrationTestBase {

		@Autowired
		UserRepository userRepository;

		@Test
		@DisplayName("The dev profile runs all DDL and DML changesets, so the test user is inserted")
		void givenDevProfile_liquibaseMigrationShouldExecuteDevContextChangesets() {
			// when
			long userCount = userRepository.count();

			// then
			assertThat(userCount).isGreaterThan(0);
		}

	}

	@Nested
	@ActiveProfiles("prod")
	class ProdProfile extends PostgresIntegrationTestBase {

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
