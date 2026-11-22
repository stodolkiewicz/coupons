package com.empik.coupons.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.entities.Coupon;
import com.empik.coupons.integration.liquibase.config.PostgresIntegrationTestBase;
import com.empik.coupons.repositories.CouponRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@AutoConfigureMockMvc
class CreateCouponIT extends PostgresIntegrationTestBase {

	private static final String COUPONS_URI = "/api/v1/coupons";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	CouponRepository couponRepository;

	@Test
	@DisplayName("A valid request creates a coupon with the code and country code in uppercase")
	void givenValidRequest_shouldCreateCoupon() {
		// given
		String body = """
				{"code": "wiosna", "countryCode": "pl", "maxUses": 10}
				""";
		Instant before = Instant.now();

		// when
		MvcTestResult result = postCoupon(body);

		// then
		assertThat(result)
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.isLenientlyEqualTo("""
						{"code": "WIOSNA", "countryCode": "PL", "maxUses": 10, "currentUses": 0}
						""");

		assertThat(result)
				.bodyJson()
				.hasPathSatisfying(
						"$.createdAt",
						createdAt -> assertThat(createdAt).isNotNull()
				);

		Coupon coupon = couponRepository.findAll().getFirst();
		assertThat(coupon.getCode()).isEqualTo("WIOSNA");
		assertThat(coupon.getCountryCode()).isEqualTo("PL");
		assertThat(coupon.getCurrentUses()).isZero();
		assertThat(coupon.getCreatedAt()).isBetween(
				before,
				Instant.now()
		);
	}

	@Test
	@DisplayName("A code that differs from an existing one only in letter case returns 409")
	void givenExistingCodeInDifferentCase_shouldReturnConflict() {
		// given
		postCoupon("""
				{"code": "WIOSNA", "countryCode": "PL", "maxUses": 10}
				""");

		// when
		MvcTestResult result = postCoupon("""
				{"code": "wiosna", "countryCode": "DE", "maxUses": 5}
				""");

		// then
		assertThat(result)
				.hasStatus(HttpStatus.CONFLICT)
				.bodyJson()
				.isLenientlyEqualTo("""
						{
						  "status": 409,
						  "title": "Coupon code already exists",
						  "detail": "Coupon with code WIOSNA already exists.",
						  "instance": "/api/v1/coupons",
						  "couponCode": "WIOSNA"
						}
						""");
		assertThat(result)
				.bodyJson()
				.hasPathSatisfying(
						"$.timestamp",
						timestamp -> assertThat(timestamp).isNotNull()
				);
		assertThat(couponRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("Malformed JSON returns 400 with the request URI and timestamp")
	void givenMalformedJson_shouldReturnBadRequest() {
		// given
		String body = "{\"code\": ";

		// when
		MvcTestResult result = postCoupon(body);

		// then
		assertThat(result)
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.isLenientlyEqualTo("""
						{"status": 400, "instance": "/api/v1/coupons"}
						""");

		assertThat(result)
				.bodyJson()
				.hasPathSatisfying(
						"$.timestamp",
						timestamp -> assertThat(timestamp).isNotNull()
				);
	}

	@ParameterizedTest(name = "{0}")
	@DisplayName("An invalid field returns 400 with the name of that field")
	@CsvSource(delimiter = '|', textBlock = """
			blank code                 | {"code": " ", "countryCode": "PL", "maxUses": 10}          | code
			code with a space          | {"code": "WIOSNA 1", "countryCode": "PL", "maxUses": 10}   | code
			missing country code       | {"code": "WIOSNA", "maxUses": 10}                          | countryCode
			unknown country code       | {"code": "WIOSNA", "countryCode": "ZZ", "maxUses": 10}     | countryCode
			zero max uses              | {"code": "WIOSNA", "countryCode": "PL", "maxUses": 0}      | maxUses
			""")
	void givenInvalidField_shouldReturnBadRequest(
			String description,
			String body,
			String invalidField
	) {
		// when
		MvcTestResult result = postCoupon(body);

		// then
		assertThat(result)
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[*].field")
				.asArray()
				.contains(invalidField);
		assertThat(couponRepository.count()).isZero();
	}

	private MvcTestResult postCoupon(String body) {
		return mvc.post()
				.uri(COUPONS_URI)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

}
