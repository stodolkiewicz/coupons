package com.empik.coupons.integration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.empik.coupons.entities.Coupon;
import com.empik.coupons.entities.User;
import com.empik.coupons.geoip.resolver.IpCountryResolverService;
import com.empik.coupons.integration.liquibase.config.PostgresIntegrationTestBase;
import com.empik.coupons.repositories.CouponRepository;
import com.empik.coupons.repositories.CouponUsageRepository;
import com.empik.coupons.repositories.UserRepository;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@AutoConfigureMockMvc
class RedeemCouponIT extends PostgresIntegrationTestBase {

	private static final String CLIENT_IP = "83.0.0.1";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	CouponRepository couponRepository;

	@Autowired
	CouponUsageRepository couponUsageRepository;

	@Autowired
	UserRepository userRepository;

	// Replaces the real resolver in the Spring context, so each test chooses the country of CLIENT_IP.
	// The mock has the type of the class, because DbIpDatabaseDownloader injects IpCountryResolverService.
	@MockitoBean
	IpCountryResolverService ipCountryResolverService;

	@Test
	void givenValidRedemption_shouldReturnCreatedAndRecordUsage() {
		// given
		User user = userRepository.save(new User(
				"Jan",
				"Kowalski"
		));
		saveCoupon(
				"WIOSNA",
				"PL",
				10
		);
		mockCountryOfClientIp("PL");

		// when
		MvcTestResult result = postUsage(
				"wiosna",
				user.getId()
		);

		// then
		assertThat(result)
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.isLenientlyEqualTo("""
						{"couponCode": "WIOSNA", "userId": "%s"}
						""".formatted(user.getId()));
		assertThat(couponRepository.findByCode("WIOSNA").orElseThrow().getCurrentUses()).isEqualTo(1);
		assertThat(couponUsageRepository.count()).isEqualTo(1);
	}

	@Test
	void givenUnknownCouponCode_shouldReturnNotFound() {
		// given
		User user = userRepository.save(new User(
				"Jan",
				"Kowalski"
		));

		// when
		MvcTestResult result = postUsage(
				"lato",
				user.getId()
		);

		// then
		assertThat(result)
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson()
				.extractingPath("$.title")
				.isEqualTo("Coupon not found");
	}

	@Test
	void givenUnknownUser_shouldReturnNotFound() {
		// given
		saveCoupon(
				"WIOSNA",
				"PL",
				10
		);

		// when
		MvcTestResult result = postUsage(
				"WIOSNA",
				UUID.randomUUID()
		);

		// then
		assertThat(result)
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson()
				.extractingPath("$.title")
				.isEqualTo("User not found");
		assertThat(couponUsageRepository.count()).isZero();
	}

	@Test
	void givenUnknownClientCountry_shouldReturnForbidden() {
		// given
		User user = userRepository.save(new User(
				"Jan",
				"Kowalski"
		));
		saveCoupon(
				"WIOSNA",
				"PL",
				10
		);
		when(ipCountryResolverService.resolveCountryCode(any(InetAddress.class))).thenReturn(Optional.empty());

		// when
		MvcTestResult result = postUsage(
				"WIOSNA",
				user.getId()
		);

		// then
		assertThat(result)
				.hasStatus(HttpStatus.FORBIDDEN)
				.bodyJson()
				.extractingPath("$.title")
				.isEqualTo("Country cannot be determined");
		assertThat(couponUsageRepository.count()).isZero();
	}

	@Test
	void givenClientFromOtherCountry_shouldReturnForbidden() {
		// given
		User user = userRepository.save(new User(
				"Jan",
				"Kowalski"
		));
		saveCoupon(
				"WIOSNA",
				"PL",
				10
		);
		mockCountryOfClientIp("DE");

		// when
		MvcTestResult result = postUsage(
				"WIOSNA",
				user.getId()
		);

		// then
		assertThat(result)
				.hasStatus(HttpStatus.FORBIDDEN)
				.bodyJson()
				.isLenientlyEqualTo("""
						{"title": "Country not allowed", "couponCountryCode": "PL", "clientCountryCode": "DE"}
						""");
		assertThat(couponUsageRepository.count()).isZero();
	}

	@Test
	void givenUserWhoAlreadyUsedCoupon_shouldReturnConflictAndKeepOneUsage() {
		// given
		User user = userRepository.save(new User(
				"Jan",
				"Kowalski"
		));
		saveCoupon(
				"WIOSNA",
				"PL",
				10
		);
		mockCountryOfClientIp("PL");
		postUsage(
				"WIOSNA",
				user.getId()
		);

		// when
		MvcTestResult result = postUsage(
				"WIOSNA",
				user.getId()
		);

		// then
		assertThat(result)
				.hasStatus(HttpStatus.CONFLICT)
				.bodyJson()
				.extractingPath("$.title")
				.isEqualTo("Coupon already used by user");
		assertThat(couponRepository.findByCode("WIOSNA").orElseThrow().getCurrentUses()).isEqualTo(1);
		assertThat(couponUsageRepository.count()).isEqualTo(1);
	}

	@Test
	void givenExhaustedCoupon_shouldReturnConflictAndRollBackUsage() {
		// given
		User user = userRepository.save(new User(
				"Jan",
				"Kowalski"
		));
		saveCoupon(
				"WIOSNA",
				"PL",
				1
		);
		mockCountryOfClientIp("PL");
		postUsage(
				"WIOSNA",
				user.getId()
		);
		User otherUser = userRepository.save(new User(
				"Anna",
				"Nowak"
		));

		// when
		MvcTestResult result = postUsage(
				"WIOSNA",
				otherUser.getId()
		);

		// then
		assertThat(result)
				.hasStatus(HttpStatus.CONFLICT)
				.bodyJson()
				.extractingPath("$.title")
				.isEqualTo("Coupon exhausted");
		assertThat(couponRepository.findByCode("WIOSNA").orElseThrow().getCurrentUses()).isEqualTo(1);
		assertThat(couponUsageRepository.count()).isEqualTo(1);
	}

	@Test
	void givenMissingUserId_shouldReturnBadRequest() {
		// when
		MvcTestResult result = mvc.post()
				.uri("/api/v1/coupons/WIOSNA/usages")
				.remoteAddress(CLIENT_IP)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange();

		// then
		assertThat(result)
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[*].field")
				.asArray()
				.containsExactly("userId");
	}

	@Test
	void givenMoreConcurrentRedemptionsThanMaxUses_shouldAcceptExactlyMaxUses() throws Exception {
		// given
		int maxUses = 5;
		int redemptions = 20;
		saveCoupon(
				"WIOSNA",
				"PL",
				maxUses
		);
		mockCountryOfClientIp("PL");
		List<UUID> userIds = new ArrayList<>();
		for (int i = 0; i < redemptions; i++) {
			userIds.add(userRepository.save(new User(
					"User",
					"Number " + i
			)).getId());
		}
		CountDownLatch startSignal = new CountDownLatch(1);

		// when
		List<Integer> statuses = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(redemptions)) {
			List<Future<Integer>> futures = new ArrayList<>();
			for (UUID userId : userIds) {
				futures.add(executor.submit(() -> {
					startSignal.await();
					return postUsage(
							"WIOSNA",
							userId
					).getResponse().getStatus();
				}));
			}
			startSignal.countDown();
			for (Future<Integer> future : futures) {
				statuses.add(future.get());
			}
		}

		// then
		assertThat(statuses).filteredOn(status -> status == HttpStatus.CREATED.value()).hasSize(maxUses);
		assertThat(statuses).filteredOn(status -> status == HttpStatus.CONFLICT.value()).hasSize(redemptions - maxUses);
		assertThat(couponRepository.findByCode("WIOSNA").orElseThrow().getCurrentUses()).isEqualTo(maxUses);
		assertThat(couponUsageRepository.count()).isEqualTo(maxUses);
	}

	private void saveCoupon(
			String code,
			String countryCode,
			int maxUses
	) {
		couponRepository.save(new Coupon(
				code,
				countryCode,
				maxUses,
				Instant.now()
		));
	}

	private void mockCountryOfClientIp(String countryCode) {
		when(ipCountryResolverService.resolveCountryCode(InetAddress.ofLiteral(CLIENT_IP))).thenReturn(Optional.of(countryCode));
	}

	private MvcTestResult postUsage(
			String couponCode,
			UUID userId
	) {
		return mvc.post()
				.uri("/api/v1/coupons/{code}/usages", couponCode)
				.remoteAddress(CLIENT_IP)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"userId": "%s"}
						""".formatted(userId))
				.exchange();
	}

}
