package com.empik.coupons.controllers;

import com.empik.coupons.dtos.CouponResponse;
import com.empik.coupons.dtos.CreateCouponRequest;
import com.empik.coupons.mappers.CouponMapper;
import com.empik.coupons.services.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController implements CouponApi {

	private final CouponService couponService;
	private final CouponMapper couponMapper;

	public CouponController(
			CouponService couponService,
			CouponMapper couponMapper
	) {
		this.couponService = couponService;
		this.couponMapper = couponMapper;
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CouponResponse createCoupon(@Valid @RequestBody CreateCouponRequest request) {
		return couponMapper.toResponse(couponService.createCoupon(request));
	}

}
