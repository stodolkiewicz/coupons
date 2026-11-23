package com.empik.coupons.geoip.health;

import com.empik.coupons.geoip.resolver.IpCountryResolverService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports DOWN while no DB-IP Lite file is loaded, because without it every coupon use fails with an unknown country.
 * The readiness group includes this indicator, so Kubernetes sends no traffic to such a pod.
 * Spring names the indicator {@code dbIpDatabase} after the bean name without the {@code HealthIndicator} suffix.
 */
@Component
public class DbIpDatabaseHealthIndicator implements HealthIndicator {

	private final IpCountryResolverService ipCountryResolverService;

	public DbIpDatabaseHealthIndicator(IpCountryResolverService ipCountryResolverService) {
		this.ipCountryResolverService = ipCountryResolverService;
	}

	@Override
	public Health health() {
		return ipCountryResolverService.isDbIpLiteDatabaseLoadedIntoMemory()
				? Health.up().build()
				: Health.down().withDetail("reason", "No DB-IP Lite file is loaded.").build();
	}

}
