package com.empik.coupons.services;

import java.util.function.Supplier;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Tells which database constraint caused a {@link DataIntegrityViolationException}.
 */
public final class ConstraintViolationDetector {

	private ConstraintViolationDetector() {
	}

	public static boolean isViolationOfConstraint(
			DataIntegrityViolationException exception,
			String constraintName
	) {
		return exception.getCause() instanceof ConstraintViolationException violation
				&& constraintName.equalsIgnoreCase(violation.getConstraintName());
	}

	/**
	 * Returns the exception to throw instead of a {@link DataIntegrityViolationException}.
	 *
	 * @return the exception from {@code exceptionForConstraint} when the given constraint caused the violation,
	 * otherwise the original {@code exception}
	 */
	public static RuntimeException translateViolationOfConstraint(
			DataIntegrityViolationException exception,
			String constraintName,
			Supplier<RuntimeException> exceptionForConstraint
	) {
		return isViolationOfConstraint(
				exception,
				constraintName
		)
				? exceptionForConstraint.get()
				: exception;
	}

}
