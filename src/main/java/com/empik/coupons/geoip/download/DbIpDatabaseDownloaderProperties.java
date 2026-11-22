package com.empik.coupons.geoip.download;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of {@link DbIpDatabaseDownloader}.
 *
 * @param directory directory where the downloader saves the DB-IP Lite file; the application needs write access to it
 * @param download  settings of the download from the DB-IP server
 */
@ConfigurationProperties("coupons.geoip")
public record DbIpDatabaseDownloaderProperties(
		Path directory,
		Download download
) {

	/**
	 * {@code coupons.geoip.download.enabled} and {@code coupons.geoip.download.cron} are read
	 * directly by the annotations of the classes in {@code com.empik.coupons.geoip.triggers}.
	 *
	 * @param baseUrl URL of the directory on the DB-IP server that holds the monthly files, without a trailing slash
	 * @param timeout maximum time to wait for the DB-IP server to respond
	 */
	public record Download(
			URI baseUrl,
			Duration timeout
	) {
	}

}
