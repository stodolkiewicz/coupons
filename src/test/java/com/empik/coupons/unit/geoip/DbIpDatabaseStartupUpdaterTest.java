package com.empik.coupons.unit.geoip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.empik.coupons.geoip.download.DbIpDatabaseDownloader;
import com.empik.coupons.geoip.triggers.DbIpDatabaseStartupUpdater;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DbIpDatabaseStartupUpdaterTest {

	// Each run(...) builds a new Spring context with the downloader mock.
	// The updater bean gets into that context only when its @ConditionalOnBooleanProperty is met.
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(
					DbIpDatabaseDownloader.class,
					() -> mock(DbIpDatabaseDownloader.class)
			)
			.withUserConfiguration(DbIpDatabaseStartupUpdater.class);

	@Test
	void givenDownloadEnabledPropertyMissing_shouldCreateStartupUpdaterBean() {
		// when
		contextRunner.run(context ->

				// then
				assertThat(context).hasSingleBean(DbIpDatabaseStartupUpdater.class)
		);
	}

	@Test
	void givenDownloadEnabledPropertyFalse_shouldNotCreateStartupUpdaterBean() {
		// when
		contextRunner
				.withPropertyValues("coupons.geoip.download.enabled=false")
				.run(context ->

						// then
						assertThat(context).doesNotHaveBean(DbIpDatabaseStartupUpdater.class)
				);
	}

}
