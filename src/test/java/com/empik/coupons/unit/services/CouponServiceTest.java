package com.empik.coupons.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.empik.coupons.dtos.CreateCouponRequest;
import com.empik.coupons.entities.Coupon;
import com.empik.coupons.exceptions.CouponCodeAlreadyExistsException;
import com.empik.coupons.mappers.CouponMapper;
import com.empik.coupons.repositories.CouponRepository;
import com.empik.coupons.services.CouponService;
import java.sql.SQLException;
import java.time.Instant;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

	private static final String COUPON_CODE_UNIQUE_CONSTRAINT = "uq_coupon_code";

	private static final CreateCouponRequest REQUEST = new CreateCouponRequest(
			"wiosna",
			"pl",
			10
	);

	@Mock
	CouponRepository couponRepository;

	CouponService couponService;

	@BeforeEach
	void createService() {
		couponService = new CouponService(
				couponRepository,
				Mappers.getMapper(CouponMapper.class),
				COUPON_CODE_UNIQUE_CONSTRAINT
		);
	}

	@Test
	void givenValidRequest_shouldSaveAndReturnCoupon() {
		// given
		when(couponRepository.saveAndFlush(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));
		Instant before = Instant.now();

		// when
		Coupon coupon = couponService.createCoupon(REQUEST);

		// then
		assertThat(coupon.getCode()).isEqualTo("WIOSNA");
		assertThat(coupon.getCountryCode()).isEqualTo("PL");
		assertThat(coupon.getMaxUses()).isEqualTo(10);
		assertThat(coupon.getCurrentUses()).isZero();
		assertThat(coupon.getCreatedAt()).isBetween(
				before,
				Instant.now()
		);
	}

	@Test
	void givenViolatedCouponCodeUniqueConstraint_shouldThrowCouponCodeAlreadyExists() {
		// given
		DataIntegrityViolationException violation = constraintViolation(COUPON_CODE_UNIQUE_CONSTRAINT);
		when(couponRepository.saveAndFlush(any(Coupon.class))).thenThrow(violation);

		// when
		Throwable thrown = catchThrowable(() -> couponService.createCoupon(REQUEST));

		// then
		assertThat(thrown)
				.isInstanceOf(CouponCodeAlreadyExistsException.class)
				.hasMessage("Coupon with code WIOSNA already exists.")
				.hasCause(violation);
	}

	@Test
	void givenViolatedOtherConstraint_shouldRethrowOriginalException() {
		// given
		DataIntegrityViolationException violation = constraintViolation("ck_coupon_max_uses");
		when(couponRepository.saveAndFlush(any(Coupon.class))).thenThrow(violation);

		// when
		Throwable thrown = catchThrowable(() -> couponService.createCoupon(REQUEST));

		// then
		assertThat(thrown).isSameAs(violation);
	}

	/**
	 * Builds the exception that Spring throws when Postgres rejects an insert because of the given constraint.
	 */
	private static DataIntegrityViolationException constraintViolation(String constraintName) {
		ConstraintViolationException hibernateException = new ConstraintViolationException(
				"constraint violated",
				new SQLException("constraint violated"),
				"insert into coupon ...",
				constraintName
		);
		return new DataIntegrityViolationException(
				"could not execute statement",
				hibernateException
		);
	}

}
