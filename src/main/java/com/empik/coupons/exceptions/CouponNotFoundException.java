package com.empik.coupons.exceptions;

import lombok.Getter;

@Getter
public class CouponNotFoundException extends RuntimeException {

	private final String code;

	public CouponNotFoundException(String code) {
		super("Coupon with code " + code + " does not exist.");
		this.code = code;
	}

}
