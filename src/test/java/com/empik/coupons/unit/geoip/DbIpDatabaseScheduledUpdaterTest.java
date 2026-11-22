package com.empik.coupons.unit.geoip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.empik.coupons.geoip.download.DbIpDatabaseDownloader;
import com.empik.coupons.geoip.triggers.DbIpDatabaseScheduledUpdater;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DbIpDatabaseScheduledUpdaterTest {

	// For creating a small Spring context.
	// withBean(... - will be always created on contextRunner.run(...)
	// .withUserConfiguration(... - will take a look at @ConditionalOnBooleanProperty annotation on DbIpDatabaseScheduledUpdater
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(
					DbIpDatabaseDownloader.class,
					() -> mock(DbIpDatabaseDownloader.class)
			)
			.withUserConfiguration(DbIpDatabaseScheduledUpdater.class);

	@Test
	void givenScheduleFired_shouldUpdateDbIpDatabase() {
		// given
		DbIpDatabaseDownloader downloader = mock(DbIpDatabaseDownloader.class);
		DbIpDatabaseScheduledUpdater updater = new DbIpDatabaseScheduledUpdater(downloader);

		// when
		updater.updateDbIpDatabaseOnSchedule();

		// then
		verify(downloader).downloadDbIpLiteFileIfCurrentMonthIsMissing();
	}

	@Test
	void givenDownloadEnabledPropertyMissing_shouldCreateScheduledUpdaterBean() {
		// when
		contextRunner.run(context ->

				// then
				assertThat(context).hasSingleBean(DbIpDatabaseScheduledUpdater.class)
		);
	}

	@Test
	void givenDownloadEnabledPropertyFalse_shouldNotCreateScheduledUpdaterBean() {
		// when
		contextRunner
				.withPropertyValues("coupons.geoip.download.enabled=false")
				.run(context ->

						// then
						assertThat(context).doesNotHaveBean(DbIpDatabaseScheduledUpdater.class)
				);
	}

}
