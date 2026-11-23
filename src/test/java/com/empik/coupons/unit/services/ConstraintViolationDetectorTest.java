package com.empik.coupons.unit.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.services.ConstraintViolationDetector;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ConstraintViolationDetectorTest {

	@Test
	void givenViolationOfNamedConstraintInOtherLetterCase_shouldDetectIt() {
		// given
		DataIntegrityViolationException exception = constraintViolation("UQ_COUPON_CODE");

		// when
		boolean detected = ConstraintViolationDetector.isViolationOfConstraint(
				exception,
				"uq_coupon_code"
		);

		// then
		assertThat(detected).isTrue();
	}

	@Test
	void givenViolationOfOtherConstraint_shouldNotDetectIt() {
		// given
		DataIntegrityViolationException exception = constraintViolation("ck_coupon_max_uses");

		// when
		boolean detected = ConstraintViolationDetector.isViolationOfConstraint(
				exception,
				"uq_coupon_code"
		);

		// then
		assertThat(detected).isFalse();
	}

	@Test
	void givenExceptionWithoutHibernateCause_shouldNotDetectIt() {
		// given
		DataIntegrityViolationException exception = new DataIntegrityViolationException("value too long");

		// when
		boolean detected = ConstraintViolationDetector.isViolationOfConstraint(
				exception,
				"uq_coupon_code"
		);

		// then
		assertThat(detected).isFalse();
	}

	@Test
	void givenViolationOfNamedConstraint_translateShouldReturnExceptionFromSupplier() {
		// given
		DataIntegrityViolationException exception = constraintViolation("uq_coupon_code");
		IllegalStateException exceptionForConstraint = new IllegalStateException("duplicate code");

		// when
		RuntimeException translated = ConstraintViolationDetector.translateViolationOfConstraint(
				exception,
				"uq_coupon_code",
				() -> exceptionForConstraint
		);

		// then
		assertThat(translated).isSameAs(exceptionForConstraint);
	}

	@Test
	void givenViolationOfOtherConstraint_translateShouldReturnOriginalException() {
		// given
		DataIntegrityViolationException exception = constraintViolation("ck_coupon_max_uses");

		// when
		RuntimeException translated = ConstraintViolationDetector.translateViolationOfConstraint(
				exception,
				"uq_coupon_code",
				() -> new IllegalStateException("duplicate code")
		);

		// then
		assertThat(translated).isSameAs(exception);
	}

	private static DataIntegrityViolationException constraintViolation(String constraintName) {
		return new DataIntegrityViolationException(
				"could not execute statement",
				new ConstraintViolationException(
						"constraint violated",
						new SQLException("constraint violated"),
						"insert into coupon ...",
						constraintName
				)
		);
	}

}
