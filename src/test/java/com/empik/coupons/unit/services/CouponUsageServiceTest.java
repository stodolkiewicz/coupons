package com.empik.coupons.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.empik.coupons.entities.Coupon;
import com.empik.coupons.entities.CouponUsage;
import com.empik.coupons.entities.User;
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
import com.empik.coupons.services.CouponUsageService;
import java.net.InetAddress;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CouponUsageServiceTest {

	private static final String COUPON_USAGE_COUPON_USER_UNIQUE_CONSTRAINT = "uq_coupon_usage_coupon_user";
	private static final InetAddress CLIENT_IP = InetAddress.ofLiteral("83.0.0.1");

	@Mock
	CouponRepository couponRepository;

	@Mock
	CouponUsageRepository couponUsageRepository;

	@Mock
	UserRepository userRepository;

	@Mock
	IpCountryResolver ipCountryResolver;

	CouponUsageService couponUsageService;

	Coupon coupon;
	User user;

	@BeforeEach
	void createServiceAndEntities() {
		couponUsageService = new CouponUsageService(
				couponRepository,
				couponUsageRepository,
				userRepository,
				ipCountryResolver,
				COUPON_USAGE_COUPON_USER_UNIQUE_CONSTRAINT
		);
		coupon = new Coupon(
				"WIOSNA",
				"PL",
				10,
				Instant.now()
		);
		user = new User(
				"Jan",
				"Kowalski"
		);
	}

	@Test
	void givenValidRedemption_shouldInsertUsageAndIncrementCurrentUses() {
		// given
		mockPolishWiosnaCouponExistingUserAndClientCountry("PL");
		when(couponUsageRepository.saveAndFlush(any(CouponUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(couponRepository.incrementCurrentUsesIfBelowMaxUses(coupon.getId())).thenReturn(1);

		// when
		CouponUsage couponUsage = couponUsageService.redeemCouponForUser(
				"wiosna",
				user.getId(),
				CLIENT_IP
		);

		// then
		assertThat(couponUsage.getCoupon()).isSameAs(coupon);
		assertThat(couponUsage.getUser()).isSameAs(user);
		assertThat(couponUsage.getUsedAt()).isNotNull();
		verify(couponRepository).incrementCurrentUsesIfBelowMaxUses(coupon.getId());
	}

	@Test
	void givenUnknownCouponCode_shouldThrowCouponNotFound() {
		// given
		when(couponRepository.findByCode("LATO")).thenReturn(Optional.empty());

		// when
		Throwable thrown = catchThrowable(() -> couponUsageService.redeemCouponForUser(
				"lato",
				user.getId(),
				CLIENT_IP
		));

		// then
		assertThat(thrown)
				.isInstanceOf(CouponNotFoundException.class)
				.hasMessage("Coupon with code LATO does not exist.");
	}

	@Test
	void givenUnknownUser_shouldThrowUserNotFound() {
		// given
		when(couponRepository.findByCode("WIOSNA")).thenReturn(Optional.of(coupon));
		when(userRepository.existsById(user.getId())).thenReturn(false);

		// when
		Throwable thrown = catchThrowable(() -> couponUsageService.redeemCouponForUser(
				"WIOSNA",
				user.getId(),
				CLIENT_IP
		));

		// then
		assertThat(thrown)
				.isInstanceOf(UserNotFoundException.class)
				.hasMessage("User with id " + user.getId() + " does not exist.");
		verify(couponUsageRepository, never()).saveAndFlush(any(CouponUsage.class));
	}

	@Test
	void givenUnknownClientCountry_shouldThrowClientCountryUnknown() {
		// given
		when(couponRepository.findByCode("WIOSNA")).thenReturn(Optional.of(coupon));
		when(userRepository.existsById(user.getId())).thenReturn(true);
		when(ipCountryResolver.resolveCountryCode(CLIENT_IP)).thenReturn(Optional.empty());

		// when
		Throwable thrown = catchThrowable(() -> couponUsageService.redeemCouponForUser(
				"WIOSNA",
				user.getId(),
				CLIENT_IP
		));

		// then
		assertThat(thrown)
				.isInstanceOf(ClientCountryUnknownException.class)
				.hasMessage("The country of IP address 83.0.0.1 cannot be determined.");
		verify(couponUsageRepository, never()).saveAndFlush(any(CouponUsage.class));
	}

	@Test
	void givenClientFromOtherCountry_shouldThrowCouponCountryNotAllowed() {
		// given
		when(couponRepository.findByCode("WIOSNA")).thenReturn(Optional.of(coupon));
		when(userRepository.existsById(user.getId())).thenReturn(true);
		when(ipCountryResolver.resolveCountryCode(CLIENT_IP)).thenReturn(Optional.of("DE"));

		// when
		Throwable thrown = catchThrowable(() -> couponUsageService.redeemCouponForUser(
				"WIOSNA",
				user.getId(),
				CLIENT_IP
		));

		// then
		assertThat(thrown)
				.isInstanceOf(CouponCountryNotAllowedException.class)
				.hasMessage("Coupon WIOSNA can be used only in PL, but the request comes from DE.");
		verify(couponUsageRepository, never()).saveAndFlush(any(CouponUsage.class));
	}

	@Test
	void givenUserWhoAlreadyUsedCoupon_shouldThrowCouponAlreadyUsedByUser() {
		// given
		mockPolishWiosnaCouponExistingUserAndClientCountry("PL");
		DataIntegrityViolationException violation = constraintViolation(COUPON_USAGE_COUPON_USER_UNIQUE_CONSTRAINT);
		when(couponUsageRepository.saveAndFlush(any(CouponUsage.class))).thenThrow(violation);

		// when
		Throwable thrown = catchThrowable(() -> couponUsageService.redeemCouponForUser(
				"WIOSNA",
				user.getId(),
				CLIENT_IP
		));

		// then
		assertThat(thrown)
				.isInstanceOf(CouponAlreadyUsedByUserException.class)
				.hasMessage("User " + user.getId() + " has already used coupon WIOSNA.")
				.hasCause(violation);
		verify(couponRepository, never()).incrementCurrentUsesIfBelowMaxUses(coupon.getId());
	}

	@Test
	void givenViolatedOtherConstraint_shouldRethrowOriginalException() {
		// given
		mockPolishWiosnaCouponExistingUserAndClientCountry("PL");
		DataIntegrityViolationException violation = constraintViolation("fk_coupon_usage_user");
		when(couponUsageRepository.saveAndFlush(any(CouponUsage.class))).thenThrow(violation);

		// when
		Throwable thrown = catchThrowable(() -> couponUsageService.redeemCouponForUser(
				"WIOSNA",
				user.getId(),
				CLIENT_IP
		));

		// then
		assertThat(thrown).isSameAs(violation);
	}

	@Test
	void givenExhaustedCoupon_shouldThrowCouponExhausted() {
		// given
		mockPolishWiosnaCouponExistingUserAndClientCountry("PL");
		when(couponUsageRepository.saveAndFlush(any(CouponUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(couponRepository.incrementCurrentUsesIfBelowMaxUses(coupon.getId())).thenReturn(0);

		// when
		Throwable thrown = catchThrowable(() -> couponUsageService.redeemCouponForUser(
				"WIOSNA",
				user.getId(),
				CLIENT_IP
		));

		// then
		assertThat(thrown)
				.isInstanceOf(CouponExhaustedException.class)
				.hasMessage("Coupon WIOSNA has reached its maximum number of uses.");
	}

	/**
	 * Makes the mocks return the coupon WIOSNA for Poland, an existing user and the given client country.
	 */
	private void mockPolishWiosnaCouponExistingUserAndClientCountry(String clientCountryCode) {
		when(couponRepository.findByCode("WIOSNA")).thenReturn(Optional.of(coupon));
		when(userRepository.existsById(user.getId())).thenReturn(true);
		when(userRepository.getReferenceById(user.getId())).thenReturn(user);
		when(ipCountryResolver.resolveCountryCode(CLIENT_IP)).thenReturn(Optional.of(clientCountryCode));
	}

	/**
	 * Builds the exception that Spring throws when Postgres rejects an insert because of the given constraint.
	 */
	private static DataIntegrityViolationException constraintViolation(String constraintName) {
		ConstraintViolationException hibernateException = new ConstraintViolationException(
				"constraint violated",
				new SQLException("constraint violated"),
				"insert into coupon_usage ...",
				constraintName
		);
		return new DataIntegrityViolationException(
				"could not execute statement",
				hibernateException
		);
	}

}
