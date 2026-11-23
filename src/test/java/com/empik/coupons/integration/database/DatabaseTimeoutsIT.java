package com.empik.coupons.integration.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.empik.coupons.integration.liquibase.config.PostgresIntegrationTestBase;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Checks the database timeouts from {@code spring.datasource.hikari} in {@code application.yaml}.
 */
class DatabaseTimeoutsIT extends PostgresIntegrationTestBase {

	@Autowired
	DataSource dataSource;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void givenApplicationConfig_connectionPoolShouldWaitAtMostTwoSecondsForConnection() {
		// when
		long connectionTimeoutMillis = ((HikariDataSource) dataSource).getConnectionTimeout();

		// then
		assertThat(connectionTimeoutMillis).isEqualTo(2000);
	}

	@Test
	void givenApplicationConfig_everyConnectionShouldHaveFiveSecondStatementTimeout() {
		// when
		String statementTimeout = jdbcTemplate.queryForObject(
				"SHOW statement_timeout",
				String.class
		);

		// then
		assertThat(statementTimeout).isEqualTo("5s");
	}

	@Test
	void givenStatementLongerThanFiveSeconds_postgresShouldCancelIt() {
		// when
		Throwable thrown = catchThrowable(() -> jdbcTemplate.execute("SELECT pg_sleep(6)"));

		// then
		assertThat(thrown).isInstanceOf(QueryTimeoutException.class);
	}

}
