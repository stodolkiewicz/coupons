package com.empik.coupons.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.Set;

public class IsoCountryCodeValidator implements ConstraintValidator<IsoCountryCode, String> {

	private static final Set<String> ISO_COUNTRY_CODES = Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2);

	@Override
	public boolean isValid(
			String value,
			ConstraintValidatorContext context
	) {
		if (value == null) {
			return true;
		}
		return ISO_COUNTRY_CODES.contains(value.toUpperCase(Locale.ROOT));
	}

}
