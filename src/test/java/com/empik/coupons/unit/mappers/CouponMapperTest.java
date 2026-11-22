package com.empik.coupons.unit.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.dtos.CouponResponse;
import com.empik.coupons.dtos.CreateCouponRequest;
import com.empik.coupons.entities.Coupon;
import com.empik.coupons.mappers.CouponMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CouponMapperTest {

	private static final Instant CREATED_AT = Instant.parse("2026-11-22T10:15:30Z");

	private final CouponMapper mapper = Mappers.getMapper(CouponMapper.class);

	@Test
	void givenCreateCouponRequest_toEntityShouldBuildNewCoupon() {
		// given
		CreateCouponRequest request = new CreateCouponRequest(
				"wiosna",
				"pl",
				10
		);

		// when
		Coupon coupon = mapper.toEntity(1
				request,
				CREATED_AT
		);

		// then
		assertThat(coupon.getId()).isNotNull();
		assertThat(coupon.getCode()).isEqualTo("WIOSNA");
		assertThat(coupon.getCountryCode()).isEqualTo("PL");
		assertThat(coupon.getMaxUses()).isEqualTo(10);
		assertThat(coupon.getCurrentUses()).isZero();
		assertThat(coupon.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void givenCoupon_toResponseShouldCopyAllFields() {
		// given
		Coupon coupon = new Coupon(
				"WIOSNA",
				"PL",
				10,
				CREATED_AT
		);

		// when
		CouponResponse response = mapper.toResponse(coupon);

		// then
		assertThat(response).isEqualTo(new CouponResponse(
				"WIOSNA",
				"PL",
				10,
				0,
				CREATED_AT
		));
	}

}
