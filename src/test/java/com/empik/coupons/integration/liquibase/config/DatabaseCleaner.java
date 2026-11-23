package com.empik.coupons.integration.liquibase.config;

import com.empik.coupons.repositories.CouponRepository;
import com.empik.coupons.repositories.CouponUsageRepository;
import com.empik.coupons.repositories.UserRepository;
import org.springframework.boot.test.context.TestComponent;

/**
 * Deletes test data between test methods.
 */
@TestComponent
public class DatabaseCleaner {

	private final CouponUsageRepository couponUsageRepository;
	private final CouponRepository couponRepository;
	private final UserRepository userRepository;

	public DatabaseCleaner(
			CouponUsageRepository couponUsageRepository,
			CouponRepository couponRepository,
			UserRepository userRepository
	) {
		this.couponUsageRepository = couponUsageRepository;
		this.couponRepository = couponRepository;
		this.userRepository = userRepository;
	}

	/**
	 * Deletes {@code coupon_usage} first, because its foreign keys point to {@code coupon} and {@code users}.
	 */
	public void deleteAllCouponUsagesCouponsAndUsers() {
		couponUsageRepository.deleteAllInBatch();
		couponRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

}
