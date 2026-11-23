package com.empik.coupons.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RedeemCouponRequest(
		@Schema(
				description = "Id of the user who uses the coupon.",
				example = "3f6c1a52-8b0e-4d7a-9c21-5e4b7f0a1d01"
		)
		@NotNull
		UUID userId
) {
}
