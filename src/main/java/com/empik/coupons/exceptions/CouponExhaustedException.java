package com.empik.coupons.exceptions;

import lombok.Getter;

/**
 * Signals that a coupon has reached its maximum number of uses.
 * It extends {@link RuntimeException}, so Spring rolls back the transaction of the redemption.
 */
@Getter
public class CouponExhaustedException extends RuntimeException {

	private final String code;

	public CouponExhaustedException(String code) {
		super("Coupon " + code + " has reached its maximum number of uses.");
		this.code = code;
	}

}
