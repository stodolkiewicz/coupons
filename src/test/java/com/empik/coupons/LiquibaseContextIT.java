package com.empik.coupons;

import static org.assertj.core.api.Assertions.assertThat;

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
			assertThat(userRepository.count()).isEqualTo(1);
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
			assertThat(userRepository.count()).isZero();
		}

	}

}
