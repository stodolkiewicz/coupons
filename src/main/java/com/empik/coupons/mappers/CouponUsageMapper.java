package com.empik.coupons.mappers;

import com.empik.coupons.dtos.CouponUsageResponse;
import com.empik.coupons.entities.CouponUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CouponUsageMapper {

	@Mapping(target = "couponCode", source = "coupon.code")
	@Mapping(target = "userId", source = "user.id")
	CouponUsageResponse toResponse(CouponUsage couponUsage);

}
