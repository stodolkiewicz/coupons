package com.empik.coupons.controllers;

import com.empik.coupons.dtos.CouponUsageResponse;
import com.empik.coupons.dtos.RedeemCouponRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/**
 * Holds the OpenAPI documentation of {@link CouponUsageController}.
 * springdoc reads the annotations from this interface.
 */
@Tag(name = "Coupon usages")
public interface CouponUsageApi {

	@Operation(
			summary = "Use a coupon",
			description = "Records that a user used a coupon. The country of the client IP address must match the country of the coupon.",
			parameters = @Parameter(
					in = ParameterIn.HEADER,
					name = "X-Forwarded-For",
					description = "Client IP address set by a trusted proxy. Locally it simulates the client IP; the example 83.0.0.1 is in Poland (PL).",
					example = "83.0.0.1",
					schema = @Schema(type = "string")
			)
	)
	@ApiResponse(
			responseCode = "201",
			description = "The use was recorded.",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = CouponUsageResponse.class)
			)
	)
	@ApiResponse(
			responseCode = "400",
			description = "The request body is malformed, a field is invalid, or the client IP address from X-Forwarded-For is not a valid IP address. For invalid fields, the errors property lists them.",
			content = @Content(
					mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)
			)
	)
	@ApiResponse(
			responseCode = "403",
			description = "The request comes from a country other than the country of the coupon, or the country of the client IP address cannot be determined.",
			content = @Content(
					mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)
			)
	)
	@ApiResponse(
			responseCode = "404",
			description = "The coupon or the user does not exist.",
			content = @Content(
					mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)
			)
	)
	@ApiResponse(
			responseCode = "409",
			description = "The user has already used the coupon, or the coupon has reached its maximum number of uses.",
			content = @Content(
					mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)
			)
	)
	@ApiResponse(
			responseCode = "500",
			description = "An unexpected error occurred.",
			content = @Content(
					mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)
			)
	)
	CouponUsageResponse redeemCoupon(
			@Parameter(description = "Coupon code. Letter case does not matter.", example = "wiosna")
			String code,
			RedeemCouponRequest request,
			@Parameter(hidden = true)
			HttpServletRequest httpRequest
	);

}
