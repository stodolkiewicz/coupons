package com.empik.coupons.geoip.resolver;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of {@link IpCountryResolverService}.
 *
 * @param directory directory from which the resolver loads the newest DB-IP Lite file
 */
@ConfigurationProperties("coupons.geoip")
public record IpCountryResolverProperties(
		Path directory
) {
}
