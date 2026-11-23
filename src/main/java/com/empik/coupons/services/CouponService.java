package com.empik.coupons.services;

import com.empik.coupons.dtos.CreateCouponRequest;
import com.empik.coupons.entities.Coupon;
import com.empik.coupons.exceptions.CouponCodeAlreadyExistsException;
import com.empik.coupons.mappers.CouponMapper;
import com.empik.coupons.repositories.CouponRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
		Coupon savedCoupon;
		try {
			savedCoupon = couponRepository.saveAndFlush(coupon);
		} catch (DataIntegrityViolationException e) {
			throw ConstraintViolationDetector.translateViolationOfConstraint(
					e,
					couponCodeUniqueConstraint,
					() -> {
						log.info(
								"Rejected creation of coupon {}: the code already exists",
								coupon.getCode()
						);
						return new CouponCodeAlreadyExistsException(
								coupon.getCode(),
								e
						);
					}
			);
		}
		log.info(
				"Created coupon {} for country {} with {} max uses",
				savedCoupon.getCode(),
				savedCoupon.getCountryCode(),
				savedCoupon.getMaxUses()
		);
		return savedCoupon;
	}

}
