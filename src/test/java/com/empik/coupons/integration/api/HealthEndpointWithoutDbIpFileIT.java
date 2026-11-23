package com.empik.coupons.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.integration.liquibase.config.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The directory does not exist, so the application starts without a DB-IP Lite file.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "coupons.geoip.directory=target/directory-without-db-ip-file")
class HealthEndpointWithoutDbIpFileIT extends PostgresIntegrationTestBase {

	@Autowired
	MockMvcTester mvc;

	@Test
	void givenNoDbIpLiteFile_readinessShouldReportDown() {
		// when
		MvcTestResult result = mvc.get()
				.uri("/actuator/health/readiness")
				.exchange();

		// then
		assertThat(result)
				.hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("DOWN");
	}

	@Test
	void givenNoDbIpLiteFile_livenessShouldStillReportUp() {
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

}
