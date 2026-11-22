package com.empik.coupons.dtos;

import com.empik.coupons.validation.IsoCountryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(
		@Schema(
				description = "Coupon code. The service stores it in uppercase, so codes that differ only in letter case are the same code.",
				example = "wiosna"
		)
		@NotBlank
		@Size(max = 64)
		@Pattern(regexp = "[A-Za-z0-9]+", message = "must contain only letters A-Z and digits")
		String code,

		@Schema(
				description = "Country where the coupon can be used, as an ISO 3166-1 alpha-2 code. Letter case does not matter.",
				example = "PL",
				minLength = 2,
				maxLength = 2
		)
		@NotNull
		@IsoCountryCode
		String countryCode,

		@Schema(
				description = "Maximum number of times the coupon can be used.",
				example = "100"
		)
		@NotNull
		@Positive
		Integer maxUses
) {
}
