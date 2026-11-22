package com.empik.coupons.services;

import com.empik.coupons.dtos.CreateCouponRequest;
import com.empik.coupons.entities.Coupon;
import com.empik.coupons.exceptions.CouponCodeAlreadyExistsException;
import com.empik.coupons.mappers.CouponMapper;
import com.empik.coupons.repositories.CouponRepository;
import java.time.Instant;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

	private final CouponRepository couponRepository;
	private final CouponMapper couponMapper;
	private final String couponCodeUniqueConstraint;

	public CouponService(
			CouponRepository couponRepository,
			CouponMapper couponMapper,
			@Value("${coupons.db.constraints.coupon-code-unique}") String couponCodeUniqueConstraint
	) {
		this.couponRepository = couponRepository;
		this.couponMapper = couponMapper;
		this.couponCodeUniqueConstraint = couponCodeUniqueConstraint;
	}

	/**
	 * Creates a coupon.
	 * The unique constraint on {@code coupon.code} detects a duplicate code.
	 * A check before the insert would let two concurrent requests with the same code both pass.
	 */
	@Transactional
	public Coupon createCoupon(CreateCouponRequest request) {
		Coupon coupon = couponMapper.toEntity(
				request,
				Instant.now()
		);
		try {
			return couponRepository.saveAndFlush(coupon);
		} catch (DataIntegrityViolationException e) {
			if (violatesConstraint(e, couponCodeUniqueConstraint)) {
				throw new CouponCodeAlreadyExistsException(coupon.getCode(), e);
			}
			throw e;
		}
	}

	private static boolean violatesConstraint(
			DataIntegrityViolationException exception,
			String constraintName
	) {
		return exception.getCause() instanceof ConstraintViolationException violation
				&& constraintName.equalsIgnoreCase(violation.getConstraintName());
	}

}
