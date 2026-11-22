package com.empik.coupons.geoip;

import com.empik.coupons.geoip.download.DbIpDatabaseDownloaderProperties;
import com.empik.coupons.geoip.resolver.IpCountryResolverProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
		IpCountryResolverProperties.class,
		DbIpDatabaseDownloaderProperties.class
})
public class GeoIpConfig {
}
