package com.empik.coupons.services;

import com.empik.coupons.entities.Coupon;
import com.empik.coupons.entities.CouponUsage;
import com.empik.coupons.exceptions.ClientCountryUnknownException;
import com.empik.coupons.exceptions.CouponAlreadyUsedByUserException;
import com.empik.coupons.exceptions.CouponCountryNotAllowedException;
import com.empik.coupons.exceptions.CouponExhaustedException;
import com.empik.coupons.exceptions.CouponNotFoundException;
import com.empik.coupons.exceptions.UserNotFoundException;
import com.empik.coupons.geoip.resolver.IpCountryResolver;
import com.empik.coupons.repositories.CouponRepository;
import com.empik.coupons.repositories.CouponUsageRepository;
import com.empik.coupons.repositories.UserRepository;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponUsageService {

	private final CouponRepository couponRepository;
	private final CouponUsageRepository couponUsageRepository;
	private final UserRepository userRepository;
	private final IpCountryResolver ipCountryResolver;
	private final String couponUsageCouponUserUniqueConstraint;

	public CouponUsageService(
			CouponRepository couponRepository,
			CouponUsageRepository couponUsageRepository,
			UserRepository userRepository,
			IpCountryResolver ipCountryResolver,
			@Value("${coupons.db.constraints.coupon-usage-coupon-user-unique}") String couponUsageCouponUserUniqueConstraint
	) {
		this.couponRepository = couponRepository;
		this.couponUsageRepository = couponUsageRepository;
		this.userRepository = userRepository;
		this.ipCountryResolver = ipCountryResolver;
		this.couponUsageCouponUserUniqueConstraint = couponUsageCouponUserUniqueConstraint;
	}

	/**
	 * Records the use of a coupon by a user.
	 * The insert into {@code coupon_usage} and the conditional increment of {@code current_uses} run in one transaction (ADR 0001).
	 * The unique constraint on {@code (coupon_id, user_id)} detects a second use by the same user.
	 * The conditional increment detects an exhausted coupon, and {@link CouponExhaustedException} rolls back the insert.
	 */
	@Transactional
	public CouponUsage redeemCouponForUser(
			String couponCode,
			UUID userId,
			InetAddress clientIp
	) {
		String normalizedCouponCode = couponCode.toUpperCase(Locale.ROOT);
		Coupon coupon = couponRepository.findByCode(normalizedCouponCode)
				.orElseThrow(() -> new CouponNotFoundException(normalizedCouponCode));
		if (!userRepository.existsById(userId)) {
			throw new UserNotFoundException(userId);
		}

		String clientCountryCode = ipCountryResolver.resolveCountryCode(clientIp)
				.orElseThrow(() -> new ClientCountryUnknownException(clientIp));
		if (!clientCountryCode.equals(coupon.getCountryCode())) {
			throw new CouponCountryNotAllowedException(
					coupon.getCode(),
					coupon.getCountryCode(),
					clientCountryCode
			);
		}

		CouponUsage couponUsage = insertCouponUsage(
				coupon,
				userId
		);

		if (couponRepository.incrementCurrentUsesIfBelowMaxUses(coupon.getId()) == 0) {
			throw new CouponExhaustedException(coupon.getCode());
		}

		return couponUsage;
	}

	private CouponUsage insertCouponUsage(
			Coupon coupon,
			UUID userId
	) {
		CouponUsage couponUsage = new CouponUsage(
				coupon,
				userRepository.getReferenceById(userId),
				Instant.now()
		);
		try {
			return couponUsageRepository.saveAndFlush(couponUsage);
		} catch (DataIntegrityViolationException e) {
			throw ConstraintViolationDetector.translateViolationOfConstraint(
					e,
					couponUsageCouponUserUniqueConstraint,
					() -> new CouponAlreadyUsedByUserException(
							coupon.getCode(),
							userId,
							e
					)
			);
		}
	}

}
