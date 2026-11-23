package com.empik.coupons.exceptions;

import lombok.Getter;

@Getter
public class CouponCountryNotAllowedException extends RuntimeException {

	private final String code;
	private final String couponCountryCode;
	private final String clientCountryCode;

	public CouponCountryNotAllowedException(
			String code,
			String couponCountryCode,
			String clientCountryCode
	) {
		super("Coupon " + code + " can be used only in " + couponCountryCode + ", but the request comes from " + clientCountryCode + ".");
		this.code = code;
		this.couponCountryCode = couponCountryCode;
		this.clientCountryCode = clientCountryCode;
	}

}
