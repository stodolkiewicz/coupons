package com.empik.coupons.geoip.triggers;

import com.empik.coupons.geoip.download.DbIpDatabaseDownloader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Updates the DB-IP Lite file once the application has started.
 * {@code coupons.geoip.download.enabled=false} turns it off, for example in integration tests.
 */
@Component
@ConditionalOnBooleanProperty(name = "coupons.geoip.download.enabled", matchIfMissing = true)
public class DbIpDatabaseStartupUpdater {

	private final DbIpDatabaseDownloader dbIpDatabaseDownloader;

	public DbIpDatabaseStartupUpdater(DbIpDatabaseDownloader dbIpDatabaseDownloader) {
		this.dbIpDatabaseDownloader = dbIpDatabaseDownloader;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void updateDbIpDatabaseOnApplicationReady() {
		dbIpDatabaseDownloader.downloadDbIpLiteFileIfCurrentMonthIsMissing();
	}

}
