package com.empik.coupons.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record CouponUsageResponse(
		@Schema(
				description = "Code of the used coupon in uppercase.",
				example = "WIOSNA"
		)
		String couponCode,

		@Schema(
				description = "Id of the user who used the coupon.",
				example = "3f6c1a52-8b0e-4d7a-9c21-5e4b7f0a1d01"
		)
		UUID userId,

		@Schema(
				description = "Time of the use in UTC.",
				example = "2026-11-22T10:15:30Z"
		)
		Instant usedAt
) {
}
