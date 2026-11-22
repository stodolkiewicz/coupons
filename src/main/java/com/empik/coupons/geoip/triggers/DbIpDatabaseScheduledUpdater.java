package com.empik.coupons.geoip.triggers;

import com.empik.coupons.geoip.download.DbIpDatabaseDownloader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Updates the DB-IP Lite file on the schedule from {@code coupons.geoip.download.cron}.
 * {@code coupons.geoip.download.enabled=false} turns it off, for example in integration tests.
 */
@Component
@ConditionalOnBooleanProperty(name = "coupons.geoip.download.enabled", matchIfMissing = true)
public class DbIpDatabaseScheduledUpdater {

	private final DbIpDatabaseDownloader dbIpDatabaseDownloader;

	public DbIpDatabaseScheduledUpdater(DbIpDatabaseDownloader dbIpDatabaseDownloader) {
		this.dbIpDatabaseDownloader = dbIpDatabaseDownloader;
	}

	@Scheduled(cron = "${coupons.geoip.download.cron}", zone = "UTC")
	public void updateDbIpDatabaseOnSchedule() {
		dbIpDatabaseDownloader.downloadDbIpLiteFileIfCurrentMonthIsMissing();
	}

}
