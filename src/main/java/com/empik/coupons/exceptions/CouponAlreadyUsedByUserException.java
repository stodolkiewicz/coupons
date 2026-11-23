package com.empik.coupons.exceptions;

import java.util.UUID;
import lombok.Getter;

@Getter
public class CouponAlreadyUsedByUserException extends RuntimeException {

	private final String code;
	private final UUID userId;

	public CouponAlreadyUsedByUserException(
			String code,
			UUID userId,
			Throwable cause
	) {
		super("User " + userId + " has already used coupon " + code + ".", cause);
		this.code = code;
		this.userId = userId;
	}

}
