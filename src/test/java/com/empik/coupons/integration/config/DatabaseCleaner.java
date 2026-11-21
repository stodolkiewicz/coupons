package com.empik.coupons.integration.config;

import com.empik.coupons.repositories.CouponRepository;
import com.empik.coupons.repositories.CouponUsageRepository;
import org.springframework.boot.test.context.TestComponent;

/**
 * Deletes test data between test methods.
 * It keeps the {@code users} table, because the dev profile inserts a user in a Liquibase migration.
 */
@TestComponent
public class DatabaseCleaner {

	private final CouponUsageRepository couponUsageRepository;
	private final CouponRepository couponRepository;

	public DatabaseCleaner(
			CouponUsageRepository couponUsageRepository,
			CouponRepository couponRepository
	) {
		this.couponUsageRepository = couponUsageRepository;
		this.couponRepository = couponRepository;
	}

	public void cleanCouponTables() {
		couponUsageRepository.deleteAllInBatch();
		couponRepository.deleteAllInBatch();
	}

}
