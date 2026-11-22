package com.empik.coupons.dtos;

import com.empik.coupons.validation.IsoCountryCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(
		@NotBlank
		@Size(max = 64)
		@Pattern(regexp = "[A-Za-z0-9]+", message = "must contain only letters A-Z and digits")
		String code,

		@NotNull
		@IsoCountryCode
		String countryCode,

		@NotNull
		@Positive
		Integer maxUses
) {
}
