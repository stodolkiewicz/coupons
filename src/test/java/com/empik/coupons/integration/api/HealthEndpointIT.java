package com.empik.coupons.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.integration.liquibase.config.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The application loads the DB-IP Lite file from the repository, so the readiness probe reports UP.
 */
@AutoConfigureMockMvc
class HealthEndpointIT extends PostgresIntegrationTestBase {

	@Autowired
	MockMvcTester mvc;

	@Test
	void givenRunningApplication_livenessShouldReportUp() {
		// when
		MvcTestResult result = mvc.get()
				.uri("/actuator/health/liveness")
				.exchange();

		// then
		assertThat(result)
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UP");
	}

	@Test
	void givenLoadedDbIpLiteFile_readinessShouldReportUp() {
		// when
		MvcTestResult result = mvc.get()
				.uri("/actuator/health/readiness")
				.exchange();

		// then
		assertThat(result)
				.hasStatus(HttpStatus.OK)
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UP");
	}

}
