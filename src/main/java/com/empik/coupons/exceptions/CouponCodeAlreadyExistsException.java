package com.empik.coupons.exceptions;

import lombok.Getter;

@Getter
public class CouponCodeAlreadyExistsException extends RuntimeException {

	private final String code;

	public CouponCodeAlreadyExistsException(
			String code,
			Throwable cause
	) {
		super("Coupon with code " + code + " already exists.", cause);
		this.code = code;
	}

}
