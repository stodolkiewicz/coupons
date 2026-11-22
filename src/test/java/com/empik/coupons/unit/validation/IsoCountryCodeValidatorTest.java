package com.empik.coupons.unit.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.validation.IsoCountryCodeValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IsoCountryCodeValidatorTest {

	private final IsoCountryCodeValidator validator = new IsoCountryCodeValidator();

	@ParameterizedTest
	@ValueSource(strings = {"PL", "pl", "De", "US"})
	void givenIsoAlpha2Code_shouldBeValid(String countryCode) {
		// when
		boolean valid = isValid(countryCode);

		// then
		assertThat(valid).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "ZZ", "P", "POL", "P1"})
	void givenNonIsoAlpha2Code_shouldBeInvalid(String countryCode) {
		// when
		boolean valid = isValid(countryCode);

		// then
		assertThat(valid).isFalse();
	}

	@Test
	void givenNull_shouldBeValid() {
		// given
		String countryCode = null;

		// when
		boolean valid = isValid(countryCode);

		// then
		assertThat(valid).isTrue();
	}

	private boolean isValid(String countryCode) {
		return validator.isValid(
				countryCode,
				null
		);
	}

}
