package com.empik.coupons.unit.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.dtos.CouponUsageResponse;
import com.empik.coupons.entities.Coupon;
import com.empik.coupons.entities.CouponUsage;
import com.empik.coupons.entities.User;
import com.empik.coupons.mappers.CouponUsageMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CouponUsageMapperTest {

	private static final Instant USED_AT = Instant.parse("2026-11-22T10:15:30Z");

	private final CouponUsageMapper mapper = Mappers.getMapper(CouponUsageMapper.class);

	@Test
	void givenCouponUsage_toResponseShouldCopyCouponCodeUserIdAndUsedAt() {
		// given
		Coupon coupon = new Coupon(
				"wiosna",
				"pl",
				10,
				USED_AT
		);
		User user = new User(
				"Jan",
				"Kowalski"
		);
		CouponUsage couponUsage = new CouponUsage(
				coupon,
				user,
				USED_AT
		);

		// when
		CouponUsageResponse response = mapper.toResponse(couponUsage);

		// then
		assertThat(response).isEqualTo(new CouponUsageResponse(
				"WIOSNA",
				user.getId(),
				USED_AT
		));
	}

}
