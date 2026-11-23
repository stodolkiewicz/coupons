package com.empik.coupons.integration.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.entities.Coupon;
import com.empik.coupons.repositories.CouponRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// @DataJpaTest starts only JPA, the repositories and Liquibase, and rolls back each test method.
// replace = NONE keeps the Postgres container below instead of an in-memory database.
// The test profile makes Liquibase run only the DDL changesets.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext
class CouponRepositoryIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired
	CouponRepository couponRepository;

	@Autowired
	TestEntityManager testEntityManager;

	@Test
	void givenCouponBelowMaxUses_incrementShouldIncreaseCurrentUsesByOne() {
		// given
		Coupon coupon = persistCouponWithCurrentUses(
				"WIOSNA",
				2,
				1
		);

		// when
		int updatedRows = couponRepository.incrementCurrentUsesIfBelowMaxUses(coupon.getId());

		// then
		assertThat(updatedRows).isEqualTo(1);
		assertThat(readCurrentUsesFromDatabase(coupon.getId())).isEqualTo(2);
	}

	@Test
	void givenCouponAtMaxUses_incrementShouldChangeNothing() {
		// given
		Coupon coupon = persistCouponWithCurrentUses(
				"WIOSNA",
				2,
				2
		);

		// when
		int updatedRows = couponRepository.incrementCurrentUsesIfBelowMaxUses(coupon.getId());

		// then
		assertThat(updatedRows).isZero();
		assertThat(readCurrentUsesFromDatabase(coupon.getId())).isEqualTo(2);
	}

	@Test
	void givenUnknownCouponId_incrementShouldUpdateNoRow() {
		// when
		int updatedRows = couponRepository.incrementCurrentUsesIfBelowMaxUses(UUID.randomUUID());

		// then
		assertThat(updatedRows).isZero();
	}

	@Test
	void givenStoredCoupon_findByCodeShouldFindItByUppercaseCode() {
		// given
		Coupon coupon = persistCouponWithCurrentUses(
				"wiosna",
				5,
				0
		);

		// when
		Optional<Coupon> found = couponRepository.findByCode("WIOSNA");

		// then
		assertThat(found).map(Coupon::getId).contains(coupon.getId());
	}

	/**
	 * Saves a coupon and sets its {@code current_uses} with SQL, because {@link Coupon} has no setter for it.
	 */
	private Coupon persistCouponWithCurrentUses(
			String code,
			int maxUses,
			int currentUses
	) {
		Coupon coupon = testEntityManager.persistFlushFind(new Coupon(
				code,
				"PL",
				maxUses,
				Instant.now()
		));
		testEntityManager.getEntityManager()
				.createNativeQuery("UPDATE coupon SET current_uses = :currentUses WHERE id = :id")
				.setParameter("currentUses", currentUses)
				.setParameter("id", coupon.getId())
				.executeUpdate();
		return coupon;
	}

	/**
	 * Clears the persistence context first, because the JPQL UPDATE bypasses it and the cached entity is stale.
	 */
	private int readCurrentUsesFromDatabase(UUID couponId) {
		testEntityManager.clear();
		return testEntityManager.find(Coupon.class, couponId).getCurrentUses();
	}

}
