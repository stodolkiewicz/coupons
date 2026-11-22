package com.empik.coupons.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record CouponResponse(
		@Schema(
				description = "Coupon code in uppercase.",
				example = "WIOSNA"
		)
		String code,

		@Schema(
				description = "Country where the coupon can be used, as an ISO 3166-1 alpha-2 code in uppercase.",
				example = "PL"
		)
		String countryCode,

		@Schema(
				description = "Maximum number of times the coupon can be used.",
				example = "100"
		)
		int maxUses,

		@Schema(
				description = "Number of times the coupon has been used so far.",
				example = "0"
		)
		int currentUses,

		@Schema(
				description = "Creation time of the coupon in UTC.",
				example = "2026-11-22T10:15:30Z"
		)
		Instant createdAt
) {
}
