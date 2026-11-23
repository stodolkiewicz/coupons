package com.empik.coupons.unit.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.exceptions.ClientCountryUnknownException;
import com.empik.coupons.exceptions.CouponAlreadyUsedByUserException;
import com.empik.coupons.exceptions.CouponCountryNotAllowedException;
import com.empik.coupons.exceptions.CouponExhaustedException;
import com.empik.coupons.exceptions.CouponNotFoundException;
import com.empik.coupons.exceptions.GeoIpLookupException;
import com.empik.coupons.exceptions.GlobalExceptionHandler;
import com.empik.coupons.exceptions.UserNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

	private static final String USAGES_URI = "/api/v1/coupons/WIOSNA/usages";
	private static final UUID USER_ID = UUID.fromString("3f6c1a52-8b0e-4d7a-9c21-5e4b7f0a1d01");

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	private final MockHttpServletRequest request = new MockHttpServletRequest(
			"POST",
			USAGES_URI
	);

	@Test
	void givenGeoIpLookupException_shouldReturnInternalServerErrorWithoutInternalDetails() throws IOException {
		// given
		GeoIpLookupException exception = new GeoIpLookupException(
				InetAddress.getByName("8.8.8.8"),
				new IOException("corrupt file")
		);
		Instant before = Instant.now();

		// when
		ProblemDetail problem = handler.handleGeoIpLookup(
				exception,
				request
		);

		// then
		assertThat(problem.getStatus()).isEqualTo(500);
		assertThat(problem.getTitle()).isEqualTo("Country lookup failed");
		assertThat(problem.getDetail()).isEqualTo("The country of the IP address cannot be determined.");
		assertThat(problem.getInstance()).isEqualTo(URI.create(USAGES_URI));
		assertThat(problem.getProperties())
				.extractingByKey("timestamp")
				.isInstanceOfSatisfying(
						Instant.class,
						timestamp -> assertThat(timestamp).isBetween(
								before,
								Instant.now()
						)
				);
	}

	@Test
	void givenCouponNotFoundException_shouldReturnNotFoundWithCouponCode() {
		// when
		ProblemDetail problem = handler.handleCouponNotFound(
				new CouponNotFoundException("WIOSNA"),
				request
		);

		// then
		assertThat(problem.getStatus()).isEqualTo(404);
		assertThat(problem.getTitle()).isEqualTo("Coupon not found");
		assertThat(problem.getDetail()).isEqualTo("Coupon with code WIOSNA does not exist.");
		assertThat(problem.getProperties()).containsEntry("couponCode", "WIOSNA");
		assertThat(problem.getInstance()).isEqualTo(URI.create(USAGES_URI));
	}

	@Test
	void givenUserNotFoundException_shouldReturnNotFoundWithUserId() {
		// when
		ProblemDetail problem = handler.handleUserNotFound(
				new UserNotFoundException(USER_ID),
				request
		);

		// then
		assertThat(problem.getStatus()).isEqualTo(404);
		assertThat(problem.getTitle()).isEqualTo("User not found");
		assertThat(problem.getProperties()).containsEntry("userId", USER_ID);
		assertThat(problem.getInstance()).isEqualTo(URI.create(USAGES_URI));
	}

	@Test
	void givenClientCountryUnknownException_shouldReturnForbidden() {
		// when
		ProblemDetail problem = handler.handleClientCountryUnknown(
				new ClientCountryUnknownException(InetAddress.ofLiteral("10.0.0.1")),
				request
		);

		// then
		assertThat(problem.getStatus()).isEqualTo(403);
		assertThat(problem.getTitle()).isEqualTo("Country cannot be determined");
		assertThat(problem.getDetail()).isEqualTo("The country of IP address 10.0.0.1 cannot be determined.");
		assertThat(problem.getInstance()).isEqualTo(URI.create(USAGES_URI));
	}

	@Test
	void givenCouponCountryNotAllowedException_shouldReturnForbiddenWithBothCountries() {
		// when
		ProblemDetail problem = handler.handleCouponCountryNotAllowed(
				new CouponCountryNotAllowedException(
						"WIOSNA",
						"PL",
						"DE"
				),
				request
		);

		// then
		assertThat(problem.getStatus()).isEqualTo(403);
		assertThat(problem.getTitle()).isEqualTo("Country not allowed");
		assertThat(problem.getProperties())
				.containsEntry("couponCode", "WIOSNA")
				.containsEntry("couponCountryCode", "PL")
				.containsEntry("clientCountryCode", "DE");
		assertThat(problem.getInstance()).isEqualTo(URI.create(USAGES_URI));
	}

	@Test
	void givenCouponAlreadyUsedByUserException_shouldReturnConflictWithCouponCodeAndUserId() {
		// when
		ProblemDetail problem = handler.handleCouponAlreadyUsedByUser(
				new CouponAlreadyUsedByUserException(
						"WIOSNA",
						USER_ID,
						new RuntimeException("unique violation")
				),
				request
		);

		// then
		assertThat(problem.getStatus()).isEqualTo(409);
		assertThat(problem.getTitle()).isEqualTo("Coupon already used by user");
		assertThat(problem.getProperties())
				.containsEntry("couponCode", "WIOSNA")
				.containsEntry("userId", USER_ID);
		assertThat(problem.getInstance()).isEqualTo(URI.create(USAGES_URI));
	}

	@Test
	void givenCouponExhaustedException_shouldReturnConflictWithCouponCode() {
		// when
		ProblemDetail problem = handler.handleCouponExhausted(
				new CouponExhaustedException("WIOSNA"),
				request
		);

		// then
		assertThat(problem.getStatus()).isEqualTo(409);
		assertThat(problem.getTitle()).isEqualTo("Coupon exhausted");
		assertThat(problem.getDetail()).isEqualTo("Coupon WIOSNA has reached its maximum number of uses.");
		assertThat(problem.getProperties()).containsEntry("couponCode", "WIOSNA");
		assertThat(problem.getInstance()).isEqualTo(URI.create(USAGES_URI));
	}

}
