package com.empik.coupons.mappers;

import com.empik.coupons.dtos.CouponResponse;
import com.empik.coupons.dtos.CreateCouponRequest;
import com.empik.coupons.entities.Coupon;
import java.time.Instant;
import org.mapstruct.Mapper;

@Mapper
public interface CouponMapper {

	/**
	 * Builds a new coupon through the public constructor of {@link Coupon}.
	 * The constructor generates the id, sets the usage counter to 0 and converts the codes to uppercase.
	 */
	Coupon toEntity(
			CreateCouponRequest request,
			Instant createdAt
	);

	CouponResponse toResponse(Coupon coupon);

}
