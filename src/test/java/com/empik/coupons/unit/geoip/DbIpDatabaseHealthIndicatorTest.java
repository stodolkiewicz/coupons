package com.empik.coupons.unit.geoip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.empik.coupons.geoip.health.DbIpDatabaseHealthIndicator;
import com.empik.coupons.geoip.resolver.IpCountryResolverService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class DbIpDatabaseHealthIndicatorTest {

	@Test
	void givenLoadedDbIpLiteDatabase_shouldReportUp() {
		// given
		IpCountryResolverService ipCountryResolverService = mock(IpCountryResolverService.class);
		when(ipCountryResolverService.isDbIpLiteDatabaseLoadedIntoMemory()).thenReturn(true);
		DbIpDatabaseHealthIndicator healthIndicator = new DbIpDatabaseHealthIndicator(ipCountryResolverService);

		// when
		Health health = healthIndicator.health();

		// then
		assertThat(health.getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void givenNoDbIpLiteDatabase_shouldReportDownWithReason() {
		// given
		IpCountryResolverService ipCountryResolverService = mock(IpCountryResolverService.class);
		when(ipCountryResolverService.isDbIpLiteDatabaseLoadedIntoMemory()).thenReturn(false);
		DbIpDatabaseHealthIndicator healthIndicator = new DbIpDatabaseHealthIndicator(ipCountryResolverService);

		// when
		Health health = healthIndicator.health();

		// then
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("reason", "No DB-IP Lite file is loaded.");
	}

}
