package com.empik.coupons.controllers;

import com.empik.coupons.dtos.CouponResponse;
import com.empik.coupons.dtos.CreateCouponRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/**
 * Holds the OpenAPI documentation of {@link CouponController}.
 * springdoc reads the annotations from this interface.
 */
@Tag(name = "Coupons")
public interface CouponApi {

	@Operation(summary = "Create a coupon")
	@ApiResponse(
			responseCode = "201",
			description = "The coupon was created.",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = CouponResponse.class)
			)
	)
	@ApiResponse(
			responseCode = "400",
			description = "The request body is malformed or a field is invalid. For invalid fields, the errors property lists them.",
			content = @Content(
					mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)
			)
	)
	@ApiResponse(
			responseCode = "409",
			description = "A coupon with the same code already exists. The comparison ignores letter case.",
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
	CouponResponse createCoupon(CreateCouponRequest request);

}
