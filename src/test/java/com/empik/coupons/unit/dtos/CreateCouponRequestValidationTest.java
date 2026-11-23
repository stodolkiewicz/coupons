package com.empik.coupons.unit.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.dtos.CreateCouponRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks the boundary values of the Bean Validation constraints on {@link CreateCouponRequest}.
 * The validator runs without Spring, the same way Spring MVC runs it for {@code @Valid}.
 */
class CreateCouponRequestValidationTest {

	private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

	@ParameterizedTest(name = "{0}")
	@MethodSource("validRequests")
	void givenValidRequest_shouldHaveNoViolations(
			String description,
			CreateCouponRequest request
	) {
		// when
		Set<ConstraintViolation<CreateCouponRequest>> violations = VALIDATOR.validate(request);

		// then
		assertThat(violations).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("requestsWithOneInvalidField")
	void givenOneInvalidField_shouldReportViolationOfThatFieldOnly(
			String description,
			CreateCouponRequest request,
			String invalidField
	) {
		// when
		Set<ConstraintViolation<CreateCouponRequest>> violations = VALIDATOR.validate(request);

		// then
		assertThat(violations)
				.extracting(violation -> violation.getPropertyPath().toString())
				.containsOnly(invalidField);
	}

	static Stream<Arguments> validRequests() {
		return Stream.of(
				Arguments.of(
						"code with 64 characters",
						new CreateCouponRequest(
								"A".repeat(64),
								"PL",
								10
						)
				),
				Arguments.of(
						"code with lowercase letters and digits",
						new CreateCouponRequest(
								"wiosna2026",
								"PL",
								10
						)
				),
				Arguments.of(
						"lowercase country code",
						new CreateCouponRequest(
								"WIOSNA",
								"pl",
								10
						)
				),
				Arguments.of(
						"max uses equal to 1",
						new CreateCouponRequest(
								"WIOSNA",
								"PL",
								1
						)
				)
		);
	}

	static Stream<Arguments> requestsWithOneInvalidField() {
		return Stream.of(
				Arguments.of(
						"code with 65 characters",
						new CreateCouponRequest(
								"A".repeat(65),
								"PL",
								10
						),
						"code"
				),
				Arguments.of(
						"missing code",
						new CreateCouponRequest(
								null,
								"PL",
								10
						),
						"code"
				),
				Arguments.of(
						"code with a Polish letter",
						new CreateCouponRequest(
								"WIOSNAŁ",
								"PL",
								10
						),
						"code"
				),
				Arguments.of(
						"code with a hyphen",
						new CreateCouponRequest(
								"WIOSNA-2026",
								"PL",
								10
						),
						"code"
				),
				Arguments.of(
						"three-letter country code",
						new CreateCouponRequest(
								"WIOSNA",
								"POL",
								10
						),
						"countryCode"
				),
				Arguments.of(
						"missing max uses",
						new CreateCouponRequest(
								"WIOSNA",
								"PL",
								null
						),
						"maxUses"
				),
				Arguments.of(
						"negative max uses",
						new CreateCouponRequest(
								"WIOSNA",
								"PL",
								-1
						),
						"maxUses"
				)
		);
	}

}
