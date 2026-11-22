package com.empik.coupons.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 64)
	private String code;

	@Column(nullable = false, length = 2)
	private String countryCode;

	@Column(nullable = false)
	private int maxUses;

	@Column(nullable = false)
	private int currentUses;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	public Coupon(
			String code,
			String countryCode,
			int maxUses,
			Instant createdAt
	) {
		this.id = UUID.randomUUID();
		this.code = code.toUpperCase(Locale.ROOT);
		this.countryCode = countryCode.toUpperCase(Locale.ROOT);
		this.maxUses = maxUses;
		this.currentUses = 0;
		this.createdAt = createdAt;
	}

}
