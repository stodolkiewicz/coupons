package com.empik.coupons.repositories;

import com.empik.coupons.entities.Coupon;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

	/**
	 * Finds a coupon by its code.
	 * The code must be in uppercase, because the database stores codes in uppercase.
	 */
	Optional<Coupon> findByCode(String code);

	/**
	 * Increases {@code current_uses} by 1 only when the coupon has unused redemptions left.
	 * The condition and the increment run in one {@code UPDATE}, so concurrent redemptions cannot exceed {@code max_uses}.
	 * See ADR 0001.
	 *
	 * @return 1 when the counter was increased, 0 when the coupon is exhausted
	 */
	@Modifying
	@Query("UPDATE Coupon c SET c.currentUses = c.currentUses + 1 WHERE c.id = :couponId AND c.currentUses < c.maxUses")
	int incrementCurrentUsesIfBelowMaxUses(@Param("couponId") UUID couponId);

}
