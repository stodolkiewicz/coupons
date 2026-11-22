package com.empik.coupons.unit.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.exceptions.GeoIpLookupException;
import com.empik.coupons.exceptions.GlobalExceptionHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void givenGeoIpLookupException_shouldReturnInternalServerErrorWithoutInternalDetails() throws IOException {
		// given
		GeoIpLookupException exception = new GeoIpLookupException(
				InetAddress.getByName("8.8.8.8"),
				new IOException("corrupt file")
		);
		MockHttpServletRequest request = new MockHttpServletRequest(
				"POST",
				"/api/v1/coupons/WIOSNA/usages"
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
		assertThat(problem.getInstance()).isEqualTo(URI.create("/api/v1/coupons/WIOSNA/usages"));
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

}
