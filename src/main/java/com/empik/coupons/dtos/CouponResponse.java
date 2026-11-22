package com.empik.coupons.dtos;

import java.time.Instant;

public record CouponResponse(
		String code,
		String countryCode,
		int maxUses,
		int currentUses,
		Instant createdAt
) {
}
