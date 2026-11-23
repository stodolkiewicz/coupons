package com.empik.coupons.dtos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated string must be an ISO 3166-1 alpha-2 country code, for example {@code PL}.
 * The check ignores letter case.
 * A {@code null} value passes this check, so combine it with {@code @NotNull}.
 */
@Documented
@Constraint(validatedBy = IsoCountryCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsoCountryCode {

	String message() default "must be an ISO 3166-1 alpha-2 country code";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
